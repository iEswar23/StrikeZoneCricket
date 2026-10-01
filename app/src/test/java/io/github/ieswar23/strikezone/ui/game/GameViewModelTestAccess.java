package io.github.ieswar23.strikezone.ui.game;

import java.util.List;
import java.util.Random;

import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.engine.BallOutcome;
import io.github.ieswar23.strikezone.engine.Bowler;

/** Exposes package-private helpers of {@link GameViewModel} to tests in other packages. */
public final class GameViewModelTestAccess {
    private GameViewModelTestAccess() {
    }

    /** Records a scripted, already-resolved ball for the delivery in flight. */
    public static BallOutcome playResolved(GameViewModel viewModel, BallOutcome outcome) {
        return viewModel.playResolved(outcome);
    }

    public static List<Bowler> selectAttack(Team team, String focus, Random random) {
        return GameViewModel.selectAttack(team, focus, random);
    }
}
