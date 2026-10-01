package io.github.ieswar23.strikezone.ui.setup;

import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.engine.Difficulty;

/** The user's current choices on the match setup screen (immutable). */
public final class SetupSelection {
    public final String teamId;
    public final String opponentId;
    public final int overs;
    public final Difficulty difficulty;
    public final GameMode mode;
    public final int quickTarget;

    public SetupSelection(String teamId, String opponentId, int overs, Difficulty difficulty, GameMode mode, int quickTarget) {
        this.teamId = teamId;
        this.opponentId = opponentId;
        this.overs = overs;
        this.difficulty = difficulty;
        this.mode = mode;
        this.quickTarget = quickTarget;
    }

    public SetupSelection withTeams(String team, String opponent) {
        return new SetupSelection(team, opponent, overs, difficulty, mode, quickTarget);
    }

    public SetupSelection withOvers(int newOvers, int newTarget) {
        return new SetupSelection(teamId, opponentId, newOvers, difficulty, mode, newTarget);
    }

    public SetupSelection withDifficulty(Difficulty newDifficulty, int newTarget) {
        return new SetupSelection(teamId, opponentId, overs, newDifficulty, mode, newTarget);
    }

    public SetupSelection withMode(GameMode newMode) {
        return new SetupSelection(teamId, opponentId, overs, difficulty, newMode, quickTarget);
    }

    public SetupSelection withTarget(int newTarget) {
        return new SetupSelection(teamId, opponentId, overs, difficulty, mode, newTarget);
    }
}
