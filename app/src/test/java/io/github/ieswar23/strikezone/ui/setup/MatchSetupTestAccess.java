package io.github.ieswar23.strikezone.ui.setup;

import java.util.List;

import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.Team;

/** Exposes package-private helpers of {@link MatchSetupViewModel} to tests in other packages. */
public final class MatchSetupTestAccess {
    private MatchSetupTestAccess() {
    }

    public static SetupSelection resolve(SetupSelection selection, List<Team> teams, DailyChallenge challenge) {
        return MatchSetupViewModel.resolve(selection, teams, challenge);
    }
}
