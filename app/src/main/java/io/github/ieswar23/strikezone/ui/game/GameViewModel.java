package io.github.ieswar23.strikezone.ui.game;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.data.repository.AchievementRepository;
import io.github.ieswar23.strikezone.data.repository.CricketRepository;
import io.github.ieswar23.strikezone.data.repository.MatchRepository;
import io.github.ieswar23.strikezone.domain.model.MatchSetup;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.engine.Ball;
import io.github.ieswar23.strikezone.engine.BallOutcome;
import io.github.ieswar23.strikezone.engine.Bowler;
import io.github.ieswar23.strikezone.engine.BowlerType;
import io.github.ieswar23.strikezone.engine.Delivery;
import io.github.ieswar23.strikezone.engine.DeliveryGenerator;
import io.github.ieswar23.strikezone.engine.FieldSetting;
import io.github.ieswar23.strikezone.engine.MatchConfig;
import io.github.ieswar23.strikezone.engine.MatchEngine;
import io.github.ieswar23.strikezone.engine.MatchStatus;
import io.github.ieswar23.strikezone.engine.OutcomeType;
import io.github.ieswar23.strikezone.engine.Over;
import io.github.ieswar23.strikezone.engine.ScoreCalculator;
import io.github.ieswar23.strikezone.engine.ShotInput;
import io.github.ieswar23.strikezone.engine.ShotResolver;
import io.github.ieswar23.strikezone.engine.TimingJudge;
import io.github.ieswar23.strikezone.util.AppExecutors;
import io.github.ieswar23.strikezone.util.Event;

/**
 * Drives one chase: generates deliveries, resolves the player's shots through {@link ShotResolver},
 * records them in the {@link MatchEngine} and persists the finished match.
 */
@HiltViewModel
public class GameViewModel extends ViewModel {
    static final long FIRST_BALL_DELAY_MS = 900L;
    static final long NEXT_BALL_DELAY_MS = 450L;
    static final long NEW_OVER_DELAY_MS = 1700L;

    private final CricketRepository cricketRepository;
    private final MatchRepository matchRepository;
    private final AchievementRepository achievementRepository;
    private final AppExecutors executors;
    private final Random random;
    private final MatchSetup setup;
    private final ShotResolver resolver;
    private final DeliveryGenerator generator;

    private final MutableLiveData<GameUiState> state = new MutableLiveData<>(GameUiState.loading());
    private final MutableLiveData<Event<DeliveryCommand>> deliveries = new MutableLiveData<>();
    private final MutableLiveData<Event<BallOutcome>> outcomes = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> banners = new MutableLiveData<>();
    private final MutableLiveData<Event<Long>> finished = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadFailed = new MutableLiveData<>(false);

    private MatchEngine engine;
    private Team batting;
    private Team bowling;
    private FieldSetting field;
    private Delivery pending;
    private boolean pitchReady;
    private boolean outcomeAnimating;
    private boolean saving;
    private Long savedMatchId;

    @Inject
    public GameViewModel(SavedStateHandle handle, CricketRepository cricketRepository,
                         MatchRepository matchRepository, AchievementRepository achievementRepository,
                         AppExecutors executors, Random random) {
        this.cricketRepository = cricketRepository;
        this.matchRepository = matchRepository;
        this.achievementRepository = achievementRepository;
        this.executors = executors;
        this.random = random;
        this.setup = MatchSetup.fromSavedState(handle);
        this.resolver = new ShotResolver(random);
        this.generator = new DeliveryGenerator(random);
        loadTeams();
    }

    public LiveData<GameUiState> getState() {
        return state;
    }

    public LiveData<Event<DeliveryCommand>> getDeliveries() {
        return deliveries;
    }

    public LiveData<Event<BallOutcome>> getOutcomes() {
        return outcomes;
    }

    public LiveData<Event<String>> getBanners() {
        return banners;
    }

    public LiveData<Event<Long>> getFinished() {
        return finished;
    }

    public LiveData<Boolean> getLoadFailed() {
        return loadFailed;
    }

    public MatchSetup getSetup() {
        return setup;
    }

    private void loadTeams() {
        executors.diskIO().execute(() -> {
            Team bat = cricketRepository.getTeamSync(setup.getBattingTeamId());
            Team bowl = cricketRepository.getTeamSync(setup.getBowlingTeamId());
            executors.mainThread().execute(() -> onTeamsLoaded(bat, bowl));
        });
    }

    private void onTeamsLoaded(Team bat, Team bowl) {
        if (bat == null || bowl == null) {
            loadFailed.setValue(true);
            return;
        }
        batting = bat;
        bowling = bowl;
        MatchConfig config = MatchConfig.standard(setup.getOvers(), setup.getTarget(), setup.getDifficulty());
        engine = new MatchEngine(config, selectAttack(bowl, setup.getBowlerFocus(), random));
        field = FieldSetting.forOver(0, config.getOvers(), engine.currentBowler().getType(), random);
        publish();
        if (pitchReady) deliverNext(FIRST_BALL_DELAY_MS);
    }

    /**
     * Picks the bowling attack, honouring a challenge's PACE / SPIN focus. Medium pacers count as
     * pace. The order is shuffled so every chase faces a different sequence.
     */
    static List<Bowler> selectAttack(Team team, String focus, Random random) {
        List<Bowler> attack = new ArrayList<>();
        for (Bowler b : team.getBowlers()) {
            boolean matches;
            if ("PACE".equalsIgnoreCase(focus)) {
                matches = b.getType() != BowlerType.SPIN;
            } else if ("SPIN".equalsIgnoreCase(focus)) {
                matches = b.getType() == BowlerType.SPIN;
            } else {
                matches = true;
            }
            if (matches) attack.add(b);
        }
        if (attack.isEmpty()) attack.addAll(team.getBowlers());
        if (attack.isEmpty()) attack.add(new Bowler(team.getCaptain() == null ? "Captain" : team.getCaptain(), BowlerType.MEDIUM));
        Collections.shuffle(attack, random);
        return attack;
    }

    /** Called when the pitch view is attached and ready to animate. */
    public void onPitchReady() {
        pitchReady = true;
        if (engine == null) return;
        if (engine.isFinished()) {
            if (savedMatchId != null) finished.setValue(new Event<>(savedMatchId));
            else saveMatch();
            return;
        }
        if (outcomeAnimating) {
            onBallAnimationDone();
        } else if (pending != null) {
            emitDelivery(FIRST_BALL_DELAY_MS);
        } else {
            deliverNext(FIRST_BALL_DELAY_MS);
        }
    }

    /** Resolves the player's shot for the ball in flight. */
    public BallOutcome playShot(ShotInput input) {
        if (engine == null || pending == null || engine.isFinished()) return null;
        return applyOutcome(resolver.resolve(pending, input, setup.getDifficulty(), field));
    }

    /**
     * Records an already-resolved result for the ball in flight, exactly as {@link #playShot} does
     * after resolving it. Lets UI tests replay a scripted innings deterministically.
     */
    @VisibleForTesting
    BallOutcome playResolved(BallOutcome outcome) {
        if (engine == null || pending == null || engine.isFinished()) return null;
        return applyOutcome(outcome);
    }

    private BallOutcome applyOutcome(BallOutcome outcome) {
        pending = null;
        engine.record(outcome);
        outcomeAnimating = true;
        publish();
        outcomes.setValue(new Event<>(outcome));
        return outcome;
    }

    /** Called when the outcome animation of the last ball has finished. */
    public void onBallAnimationDone() {
        if (!outcomeAnimating || engine == null) return;
        outcomeAnimating = false;
        if (engine.isFinished()) {
            saveMatch();
            return;
        }
        if (engine.lastBallCompletedOver()) {
            Over finishedOver = engine.getOvers().get(engine.getOvers().size() - 1);
            Bowler next = engine.currentBowler();
            field = FieldSetting.forOver(engine.currentOverIndex(), engine.getConfig().getOvers(), next.getType(), random);
            banners.setValue(new Event<>("End of over " + (finishedOver.getIndex() + 1) + " · "
                    + finishedOver.runs() + (finishedOver.runs() == 1 ? " run" : " runs")
                    + "\nNext: " + next.getName() + " (" + next.getType().getLabel() + ") · "
                    + field.getPlan().getLabel() + " field"));
            publish();
            deliverNext(NEW_OVER_DELAY_MS);
        } else {
            deliverNext(NEXT_BALL_DELAY_MS);
        }
    }

    private void deliverNext(long delayMs) {
        pending = generator.next(engine.currentBowler().getType(), setup.getDifficulty());
        emitDelivery(delayMs);
    }

    private void emitDelivery(long delayMs) {
        BowlerType type = pending.getBowlerType();
        deliveries.setValue(new Event<>(new DeliveryCommand(pending, field, engine.currentBowler().getName(), delayMs,
                TimingJudge.perfectWindowMs(setup.getDifficulty(), type),
                TimingJudge.goodWindowMs(setup.getDifficulty(), type))));
    }

    private void saveMatch() {
        if (saving || savedMatchId != null) return;
        saving = true;
        // Badges are checked before leaving the game so the summary can show what this match unlocked.
        matchRepository.save(buildEntity(), id -> achievementRepository.checkUnlocks(id, unlocked -> {
            savedMatchId = id;
            saving = false;
            finished.setValue(new Event<>(id));
        }));
    }

    MatchEntity buildEntity() {
        MatchConfig c = engine.getConfig();
        MatchEntity e = new MatchEntity();
        e.playedAt = System.currentTimeMillis();
        e.mode = setup.getMode().name();
        e.challengeId = setup.getChallengeId();
        e.bowlerFocus = setup.getBowlerFocus();
        e.battingTeamId = batting.getId();
        e.battingTeamName = batting.getName();
        e.battingShort = batting.getShortName();
        e.battingColor = batting.getPrimaryColor();
        e.bowlingTeamId = bowling.getId();
        e.bowlingTeamName = bowling.getName();
        e.bowlingShort = bowling.getShortName();
        e.bowlingColor = bowling.getPrimaryColor();
        e.overs = c.getOvers();
        e.wicketsLimit = c.getWicketsLimit();
        e.target = c.getTarget();
        e.difficulty = c.getDifficulty().name();
        e.runs = engine.getRuns();
        e.wickets = engine.getWickets();
        e.balls = engine.getLegalBalls();
        e.fours = engine.getFours();
        e.sixes = engine.getSixes();
        e.dots = engine.getDots();
        e.extras = engine.getExtras();
        e.perfectShots = engine.getPerfectShots();
        e.maxSixesInOver = engine.maxSixesInOver();
        e.scoringOvers = engine.scoringOvers();
        e.won = engine.getStatus() == MatchStatus.WON;
        e.tied = engine.getStatus() == MatchStatus.TIED;
        e.resultText = ScoreCalculator.resultText(engine, batting.getName(), bowling.getName());
        int[] runs = engine.runsPerOver();
        int[] wkts = engine.wicketsPerOver();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < runs.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(runs[i]).append(':').append(wkts[i]);
        }
        e.overSummary = sb.toString();
        return e;
    }

    private void publish() {
        MatchConfig c = engine.getConfig();
        List<OutcomeType> chips = new ArrayList<>();
        Over current = engine.currentOver();
        // Right after an over completes the engine has already opened the next (empty) over;
        // keep showing the completed over's balls until the next delivery is bowled.
        if (current.isEmpty() && !engine.getOvers().isEmpty() && engine.lastBallCompletedOver()) {
            current = engine.getOvers().get(engine.getOvers().size() - 1);
        }
        for (Ball b : current.getBalls()) chips.add(b.getType());
        Bowler bowler = engine.currentBowler();
        state.setValue(new GameUiState(false,
                batting.getName(), batting.getShortName(), batting.getPrimaryColor(),
                bowling.getName(), bowling.getShortName(), bowling.getPrimaryColor(),
                engine.getRuns(), engine.getWickets(), c.getWicketsLimit(), engine.getLegalBalls(), c.getOvers(),
                c.getTarget(), ScoreCalculator.formatOvers(engine.getLegalBalls()),
                engine.isFinished() ? ScoreCalculator.resultText(engine, batting.getName(), bowling.getName())
                        : ScoreCalculator.chaseEquation(engine.getRunsNeeded(), engine.getBallsRemaining()),
                ScoreCalculator.formatRate(ScoreCalculator.runRate(engine.getRuns(), engine.getLegalBalls())),
                ScoreCalculator.formatRate(ScoreCalculator.requiredRunRate(engine.getRunsNeeded(), engine.getBallsRemaining())),
                chips, bowler.getName(), bowler.getType().getLabel(), field.getPlan().getLabel(),
                setup.getMode().getLabel(), engine.getStatus(),
                ScoreCalculator.resultText(engine, batting.getName(), bowling.getName())));
    }
}
