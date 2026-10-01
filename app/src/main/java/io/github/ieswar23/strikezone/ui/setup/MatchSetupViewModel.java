package io.github.ieswar23.strikezone.ui.setup;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.ieswar23.strikezone.data.repository.CricketRepository;
import io.github.ieswar23.strikezone.data.settings.AppSettings;
import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.domain.model.MatchSetup;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.engine.MatchRules;
import io.github.ieswar23.strikezone.engine.TargetGenerator;
import io.github.ieswar23.strikezone.util.Formatters;
import io.github.ieswar23.strikezone.util.LiveDataUtils;
import io.github.ieswar23.strikezone.util.SyncState;
import io.github.ieswar23.strikezone.util.UiState;

@HiltViewModel
public class MatchSetupViewModel extends ViewModel {
    private final CricketRepository repository;
    private final AppSettings settings;
    private final Random random;
    private final MutableLiveData<SetupSelection> selection;
    private final MediatorLiveData<SetupUiState> state = new MediatorLiveData<>();
    private final LiveData<UiState<List<Team>>> teams;
    private final LiveData<DailyChallenge> challenge;

    @Inject
    public MatchSetupViewModel(SavedStateHandle handle, CricketRepository repository, AppSettings settings, Random random) {
        this.repository = repository;
        this.settings = settings;
        this.random = random;
        GameMode mode = GameMode.fromName(handle.get(MatchSetupFragment.ARG_MODE));
        int overs = settings.getDefaultOvers();
        Difficulty difficulty = settings.getDefaultDifficulty();
        selection = new MutableLiveData<>(new SetupSelection(settings.getFavouriteTeamId(), null, overs, difficulty, mode,
                TargetGenerator.quickChaseTarget(overs, difficulty, random)));
        teams = LiveDataUtils.combine(repository.observeTeams(), repository.syncState(), MatchSetupViewModel::teamsState);
        challenge = LiveDataUtils.map(repository.observeChallenges(), list -> DailyChallenge.forDay(list, Formatters.dayOfYear()));
        state.addSource(teams, t -> recompute());
        state.addSource(challenge, c -> recompute());
        state.addSource(selection, s -> recompute());
    }

    static UiState<List<Team>> teamsState(List<Team> list, SyncState sync) {
        if (list != null && !list.isEmpty()) return UiState.content(list);
        if (sync != null && sync.getStatus() == SyncState.Status.FAILED) return UiState.error(sync.getMessage(), null);
        return UiState.loading(null);
    }

    public LiveData<SetupUiState> getState() {
        return state;
    }

    private void recompute() {
        UiState<List<Team>> t = teams.getValue();
        if (t == null) t = UiState.loading(null);
        DailyChallenge c = challenge.getValue();
        SetupSelection resolved = resolve(selection.getValue(), t.getData(), c);
        boolean daily = resolved.mode == GameMode.DAILY_CHALLENGE && c != null;
        int target = daily ? c.getTarget() : resolved.quickTarget;
        state.setValue(new SetupUiState(t, resolved, c, target, MatchRules.wicketsForOvers(resolved.overs)));
    }

    /**
     * Fills in defaults and enforces the rules: two different teams, and in Daily Challenge mode
     * the opponent, overs and difficulty come from the challenge.
     */
    static SetupSelection resolve(SetupSelection sel, List<Team> teams, DailyChallenge challenge) {
        if (teams == null || teams.isEmpty()) return sel;
        String team = contains(teams, sel.teamId) ? sel.teamId : teams.get(0).getId();
        if (sel.mode == GameMode.DAILY_CHALLENGE && challenge != null) {
            String opponent = challenge.getOpponentId();
            if (team.equals(opponent)) team = firstOther(teams, opponent);
            return new SetupSelection(team, opponent, challenge.getOvers(), challenge.getDifficulty(), sel.mode, sel.quickTarget);
        }
        String opponent = contains(teams, sel.opponentId) && !team.equals(sel.opponentId) ? sel.opponentId : firstOther(teams, team);
        return new SetupSelection(team, opponent, sel.overs, sel.difficulty, sel.mode, sel.quickTarget);
    }

    private static boolean contains(List<Team> teams, String id) {
        if (id == null) return false;
        for (Team t : teams) if (t.getId().equals(id)) return true;
        return false;
    }

    private static String firstOther(List<Team> teams, String id) {
        for (Team t : teams) if (!t.getId().equals(id)) return t.getId();
        return id;
    }

    /** The resolved selection currently shown on screen. */
    private SetupSelection current() {
        SetupUiState s = state.getValue();
        return s != null ? s.selection : selection.getValue();
    }

    /**
     * The user's raw choices with the on-screen team ids; overs and difficulty stay the user's own
     * so leaving Daily Challenge mode restores them.
     */
    private SetupSelection raw() {
        SetupSelection cur = current();
        SetupSelection raw = selection.getValue();
        return raw == null ? cur : raw.withTeams(cur.teamId, cur.opponentId);
    }

    public void selectTeam(String id) {
        SetupSelection cur = current();
        String opponent = id.equals(cur.opponentId) ? cur.teamId : cur.opponentId;
        selection.setValue(raw().withTeams(id, opponent));
    }

    public void selectOpponent(String id) {
        SetupSelection cur = current();
        if (cur.mode == GameMode.DAILY_CHALLENGE) return;
        String team = id.equals(cur.teamId) ? cur.opponentId : cur.teamId;
        selection.setValue(raw().withTeams(team, id));
    }

    public void setOvers(int overs) {
        SetupSelection raw = raw();
        if (raw.overs == overs) return;
        selection.setValue(raw.withOvers(overs, TargetGenerator.quickChaseTarget(overs, raw.difficulty, random)));
    }

    public void setDifficulty(Difficulty difficulty) {
        SetupSelection raw = raw();
        if (raw.difficulty == difficulty) return;
        selection.setValue(raw.withDifficulty(difficulty, TargetGenerator.quickChaseTarget(raw.overs, difficulty, random)));
    }

    public void setMode(GameMode mode) {
        SetupSelection raw = raw();
        if (raw.mode == mode) return;
        selection.setValue(raw.withMode(mode));
    }

    public void rerollTarget() {
        SetupSelection raw = raw();
        selection.setValue(raw.withTarget(TargetGenerator.quickChaseTarget(raw.overs, raw.difficulty, random)));
    }

    /** Builds the navigation arguments for the game, or null if teams have not loaded yet. */
    public MatchSetup buildSetup() {
        SetupUiState s = state.getValue();
        if (s == null || s.findTeam(s.selection.teamId) == null || s.findTeam(s.selection.opponentId) == null) return null;
        SetupSelection sel = s.selection;
        settings.setFavouriteTeamId(sel.teamId);
        if (sel.mode == GameMode.DAILY_CHALLENGE && s.challenge != null) {
            DailyChallenge c = s.challenge;
            return new MatchSetup(sel.teamId, sel.opponentId, c.getOvers(), c.getTarget(), c.getDifficulty(),
                    GameMode.DAILY_CHALLENGE, c.getId(), c.getBowlerFocus());
        }
        return new MatchSetup(sel.teamId, sel.opponentId, sel.overs, sel.quickTarget, sel.difficulty,
                GameMode.QUICK_CHASE, null, null);
    }

    public void retry() {
        repository.refresh(true);
    }
}
