package io.github.ieswar23.strikezone.ui.setup;

import java.util.List;

import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.util.UiState;

/** Everything the setup screen renders. */
public final class SetupUiState {
    public final UiState<List<Team>> teams;
    public final SetupSelection selection;
    public final DailyChallenge challenge;
    public final int target;
    public final int wickets;

    public SetupUiState(UiState<List<Team>> teams, SetupSelection selection, DailyChallenge challenge, int target, int wickets) {
        this.teams = teams;
        this.selection = selection;
        this.challenge = challenge;
        this.target = target;
        this.wickets = wickets;
    }

    public Team findTeam(String id) {
        List<Team> list = teams.getData();
        if (list == null || id == null) return null;
        for (Team t : list) if (t.getId().equals(id)) return t;
        return null;
    }
}
