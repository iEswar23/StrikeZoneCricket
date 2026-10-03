package io.github.ieswar23.strikezone.ui;

import static com.google.common.truth.Truth.assertThat;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.SavedStateHandle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.domain.achievements.Achievement;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.domain.model.MatchSetup;
import io.github.ieswar23.strikezone.engine.BallOutcome;
import io.github.ieswar23.strikezone.engine.Bowler;
import io.github.ieswar23.strikezone.engine.BowlerType;
import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.engine.ShotInput;
import io.github.ieswar23.strikezone.ui.FakeRepositories.FakeAchievementRepository;
import io.github.ieswar23.strikezone.ui.FakeRepositories.FakeCricketRepository;
import io.github.ieswar23.strikezone.ui.FakeRepositories.FakeMatchRepository;
import io.github.ieswar23.strikezone.ui.game.DeliveryCommand;
import io.github.ieswar23.strikezone.ui.game.GameUiState;
import io.github.ieswar23.strikezone.ui.game.GameViewModel;
import io.github.ieswar23.strikezone.ui.game.GameViewModelTestAccess;
import io.github.ieswar23.strikezone.util.AppExecutors;
import io.github.ieswar23.strikezone.util.Event;

public class GameViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantExecutor = new InstantTaskExecutorRule();

    private FakeCricketRepository cricket;
    private FakeMatchRepository matches;
    private FakeAchievementRepository achievements;
    private AppExecutors executors;

    @Before
    public void setUp() {
        cricket = new FakeCricketRepository();
        matches = new FakeMatchRepository();
        achievements = new FakeAchievementRepository();
        executors = new AppExecutors(Runnable::run, Runnable::run);
    }

    private GameViewModel create(String batting, String bowling, int overs, int target, String focus) {
        MatchSetup setup = new MatchSetup(batting, bowling, overs, target, Difficulty.MEDIUM,
                focus == null ? GameMode.QUICK_CHASE : GameMode.DAILY_CHALLENGE, focus == null ? null : "dc-002", focus);
        Map<String, Object> args = new HashMap<>();
        args.put(MatchSetup.ARG_BATTING, setup.getBattingTeamId());
        args.put(MatchSetup.ARG_BOWLING, setup.getBowlingTeamId());
        args.put(MatchSetup.ARG_OVERS, setup.getOvers());
        args.put(MatchSetup.ARG_TARGET, setup.getTarget());
        args.put(MatchSetup.ARG_DIFFICULTY, setup.getDifficulty().name());
        args.put(MatchSetup.ARG_MODE, setup.getMode().name());
        args.put(MatchSetup.ARG_CHALLENGE, setup.getChallengeId());
        args.put(MatchSetup.ARG_BOWLER_FOCUS, setup.getBowlerFocus());
        return new GameViewModel(new SavedStateHandle(args), cricket, matches, achievements, executors,
                new Random(2026));
    }

    /** Plays until the match is saved, swinging with the line on every ball (or leaving everything). */
    private int playOut(GameViewModel vm, boolean swing) {
        vm.onPitchReady();
        int balls = 0;
        while (matches.saved.isEmpty() && balls < 500) {
            Event<DeliveryCommand> event = vm.getDeliveries().getValue();
            assertThat(event).isNotNull();
            DeliveryCommand command = event.peek();
            ShotInput input = swing
                    ? ShotInput.swing(0, command.delivery.getLine().getNaturalDirection())
                    : ShotInput.leave();
            BallOutcome outcome = vm.playShot(input);
            assertThat(outcome).isNotNull();
            vm.onBallAnimationDone();
            balls++;
        }
        return balls;
    }

    @Test
    public void firstDeliveryIsEmittedOncePitchIsReady() {
        GameViewModel vm = create("mum", "col", 2, 25, null);
        GameUiState state = vm.getState().getValue();
        assertThat(state.loading).isFalse();
        assertThat(state.battingName).isEqualTo("Mumbai Mavericks");
        assertThat(state.wicketsLimit).isEqualTo(3);
        assertThat(state.equationText).isEqualTo("Need 25 runs from 12 balls");
        assertThat(vm.getDeliveries().getValue()).isNull();

        vm.onPitchReady();
        DeliveryCommand command = vm.getDeliveries().getValue().consume();
        assertThat(command).isNotNull();
        assertThat(command.goodWindowMs).isGreaterThan(command.perfectWindowMs);
    }

    @Test
    public void completedChaseIsSavedWithAConsistentScorecard() {
        GameViewModel vm = create("mum", "col", 2, 25, null);
        playOut(vm, true);

        assertThat(matches.saved).hasSize(1);
        MatchEntity saved = matches.saved.get(0);
        GameUiState state = vm.getState().getValue();
        assertThat(saved.runs).isEqualTo(state.runs);
        assertThat(saved.wickets).isEqualTo(state.wickets);
        assertThat(saved.won).isEqualTo(saved.runs >= 25);
        assertThat(saved.balls).isAtMost(12);
        assertThat(saved.battingTeamName).isEqualTo("Mumbai Mavericks");
        assertThat(saved.bowlingTeamName).isEqualTo("Colombo Cyclones");
        assertThat(saved.resultText).isEqualTo(state.resultText);
        assertThat(saved.overSummary).isNotEmpty();
        assertThat(vm.getFinished().getValue().consume()).isEqualTo(saved.id);
    }

    @Test
    public void savedMatchRecordsTheShotFactsBadgesAreJudgedOn() {
        GameViewModel vm = create("mum", "col", 2, 25, null);
        playOut(vm, true);
        MatchEntity saved = matches.saved.get(0);
        assertThat(saved.perfectShots).isAtLeast(0);
        assertThat(saved.perfectShots).isAtMost(saved.balls + saved.extras);
        assertThat(saved.maxSixesInOver).isAtMost(saved.sixes);
        if (saved.sixes > 0) assertThat(saved.maxSixesInOver).isAtLeast(1);
        assertThat(saved.scoringOvers).isAtMost(saved.overs);
    }

    @Test
    public void badgesAreCheckedForTheSavedMatchBeforeTheSummaryOpens() {
        achievements.nextUnlocks = Collections.singletonList(Achievement.FIRST_WIN);
        GameViewModel vm = create("mum", "col", 1, 12, null);
        playOut(vm, true);
        MatchEntity saved = matches.saved.get(0);
        assertThat(achievements.checkedMatchIds).containsExactly(saved.id);
        assertThat(vm.getFinished().getValue().consume()).isEqualTo(saved.id);

        // Re-attaching the pitch after the match is over re-sends the result without re-checking.
        vm.onPitchReady();
        assertThat(achievements.checkedMatchIds).hasSize(1);
    }

    @Test
    public void leavingEveryBallLosesTheChase() {
        GameViewModel vm = create("mum", "col", 1, 30, null);
        playOut(vm, false);
        MatchEntity saved = matches.saved.get(0);
        assertThat(saved.won).isFalse();
        assertThat(saved.fours + saved.sixes).isEqualTo(0);
        assertThat(saved.resultText).contains("Colombo Cyclones won by");
    }

    @Test
    public void missingTeamReportsLoadFailure() {
        GameViewModel vm = create("mum", "unknown", 1, 12, null);
        assertThat(vm.getLoadFailed().getValue()).isTrue();
        assertThat(vm.getState().getValue().loading).isTrue();
    }

    @Test
    public void spinChallengeOnlyUsesSpinners() {
        List<Bowler> attack = GameViewModelTestAccess.selectAttack(FakeRepositories.COLOMBO, "SPIN", new Random(1));
        assertThat(attack).hasSize(2);
        for (Bowler b : attack) assertThat(b.getType()).isEqualTo(BowlerType.SPIN);

        List<Bowler> pace = GameViewModelTestAccess.selectAttack(FakeRepositories.COLOMBO, "PACE", new Random(1));
        for (Bowler b : pace) assertThat(b.getType()).isNotEqualTo(BowlerType.SPIN);
        assertThat(pace).hasSize(2);
    }
}
