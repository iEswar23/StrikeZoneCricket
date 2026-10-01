package io.github.ieswar23.strikezone.ui;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.ui.setup.MatchSetupTestAccess;
import io.github.ieswar23.strikezone.ui.setup.SetupSelection;

public class MatchSetupResolveTest {

    private final List<Team> teams = Arrays.asList(FakeRepositories.MUMBAI, FakeRepositories.COLOMBO);

    @Test
    public void defaultsPickTwoDifferentTeams() {
        SetupSelection raw = new SetupSelection(null, null, 2, Difficulty.MEDIUM, GameMode.QUICK_CHASE, 25);
        SetupSelection resolved = MatchSetupTestAccess.resolve(raw, teams, null);
        assertThat(resolved.teamId).isEqualTo("mum");
        assertThat(resolved.opponentId).isEqualTo("col");

        SetupSelection clash = new SetupSelection("col", "col", 2, Difficulty.MEDIUM, GameMode.QUICK_CHASE, 25);
        SetupSelection fixed = MatchSetupTestAccess.resolve(clash, teams, null);
        assertThat(fixed.teamId).isEqualTo("col");
        assertThat(fixed.opponentId).isEqualTo("mum");
    }

    @Test
    public void dailyChallengeDictatesOpponentOversAndDifficulty() {
        DailyChallenge challenge = new DailyChallenge("dc-002", 1, "Spin Web in Colombo", "Chase 24", 2, 24,
                Difficulty.HARD, "mum", "SPIN", "🌀");
        SetupSelection raw = new SetupSelection("mum", "col", 5, Difficulty.EASY, GameMode.DAILY_CHALLENGE, 60);
        SetupSelection resolved = MatchSetupTestAccess.resolve(raw, teams, challenge);
        assertThat(resolved.opponentId).isEqualTo("mum");
        assertThat(resolved.teamId).isEqualTo("col");
        assertThat(resolved.overs).isEqualTo(2);
        assertThat(resolved.difficulty).isEqualTo(Difficulty.HARD);

        assertThat(DailyChallenge.forDay(Arrays.asList(challenge), 275)).isSameInstanceAs(challenge);
        assertThat(DailyChallenge.forDay(Arrays.asList(), 3)).isNull();
    }
}
