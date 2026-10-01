package io.github.ieswar23.strikezone.engine;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class MatchEngineTest {

    private static final Bowler PACER = new Bowler("Rohan Kulkarni", BowlerType.PACE);
    private static final Bowler SPINNER = new Bowler("Siddharth Rane", BowlerType.SPIN);

    private static MatchEngine engine(int overs, int target) {
        return new MatchEngine(MatchConfig.standard(overs, target, Difficulty.MEDIUM), Arrays.asList(PACER, SPINNER));
    }

    private static void record(MatchEngine engine, OutcomeType... types) {
        for (OutcomeType t : types) engine.record(t, t.name());
    }

    @Test
    public void sixLegalBallsCompleteAnOverButWidesDoNot() {
        MatchEngine e = engine(2, 40);
        record(e, OutcomeType.DOT, OutcomeType.ONE, OutcomeType.DOT, OutcomeType.TWO, OutcomeType.DOT, OutcomeType.WIDE);
        assertThat(e.getLegalBalls()).isEqualTo(5);
        assertThat(e.currentOver().isComplete()).isFalse();
        assertThat(e.lastBallCompletedOver()).isFalse();

        record(e, OutcomeType.FOUR);
        assertThat(e.getLegalBalls()).isEqualTo(6);
        assertThat(e.lastBallCompletedOver()).isTrue();
        assertThat(e.currentOverIndex()).isEqualTo(1);
        assertThat(e.getOvers().get(0).getBalls()).hasSize(7);
        assertThat(ScoreCalculator.formatOvers(e.getLegalBalls())).isEqualTo("1.0");
    }

    @Test
    public void chaseIsWonTheMomentTheTargetIsReached() {
        MatchEngine e = engine(2, 10);
        record(e, OutcomeType.SIX);
        assertThat(e.getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
        record(e, OutcomeType.FOUR);
        assertThat(e.getStatus()).isEqualTo(MatchStatus.WON);
        assertThat(e.getRunsNeeded()).isEqualTo(0);
        assertThat(e.getBallsRemaining()).isEqualTo(10);
        assertThrows(IllegalStateException.class, () -> e.record(OutcomeType.DOT, "too late"));
    }

    @Test
    public void losingAllWicketsEndsTheInnings() {
        MatchEngine e = engine(1, 20);
        assertThat(e.getConfig().getWicketsLimit()).isEqualTo(2);
        record(e, OutcomeType.FOUR, OutcomeType.CAUGHT);
        assertThat(e.isFinished()).isFalse();
        record(e, OutcomeType.BOWLED);
        assertThat(e.getStatus()).isEqualTo(MatchStatus.LOST);
        assertThat(e.getBallsRemaining()).isEqualTo(3);
        assertThat(e.getWicketsRemaining()).isEqualTo(0);
    }

    @Test
    public void finishingOneRunShortAfterAllBallsIsATie() {
        MatchEngine e = engine(1, 7);
        record(e, OutcomeType.ONE, OutcomeType.ONE, OutcomeType.ONE, OutcomeType.ONE, OutcomeType.ONE);
        assertThat(e.isFinished()).isFalse();
        record(e, OutcomeType.ONE);
        assertThat(e.getStatus()).isEqualTo(MatchStatus.TIED);
    }

    @Test
    public void runningOutOfBallsShortOfTargetIsALoss() {
        MatchEngine e = engine(1, 30);
        for (int i = 0; i < 6; i++) record(e, OutcomeType.TWO);
        assertThat(e.getStatus()).isEqualTo(MatchStatus.LOST);
        assertThat(e.getRuns()).isEqualTo(12);
    }

    @Test
    public void bowlersRotateEveryOver() {
        MatchEngine e = new MatchEngine(MatchConfig.standard(5, 200, Difficulty.EASY), Arrays.asList(PACER, SPINNER));
        assertThat(e.currentBowler()).isSameInstanceAs(PACER);
        for (int i = 0; i < 6; i++) record(e, OutcomeType.ONE);
        assertThat(e.currentBowler()).isSameInstanceAs(SPINNER);
        for (int i = 0; i < 6; i++) record(e, OutcomeType.ONE);
        assertThat(e.currentBowler()).isSameInstanceAs(PACER);
    }

    @Test
    public void tracksBoundariesDotsExtrasAndPerOverBreakdown() {
        MatchEngine e = engine(2, 100);
        record(e, OutcomeType.SIX, OutcomeType.FOUR, OutcomeType.DOT, OutcomeType.WIDE, OutcomeType.RUN_OUT,
                OutcomeType.DOT, OutcomeType.SIX);
        record(e, OutcomeType.FOUR, OutcomeType.CAUGHT);
        assertThat(e.getSixes()).isEqualTo(2);
        assertThat(e.getFours()).isEqualTo(2);
        assertThat(e.getDots()).isEqualTo(2);
        assertThat(e.getExtras()).isEqualTo(1);
        assertThat(e.getRuns()).isEqualTo(6 + 4 + 1 + 1 + 6 + 4);
        assertThat(e.getBatRuns()).isEqualTo(e.getRuns() - 1);
        assertThat(e.getWickets()).isEqualTo(2);
        assertThat(e.runsPerOver()).asList().containsExactly(18, 4).inOrder();
        assertThat(e.wicketsPerOver()).asList().containsExactly(1, 1).inOrder();
    }

    @Test
    public void wicketAllowanceScalesWithOvers() {
        assertThat(MatchRules.wicketsForOvers(1)).isEqualTo(2);
        assertThat(MatchRules.wicketsForOvers(2)).isEqualTo(3);
        assertThat(MatchRules.wicketsForOvers(5)).isEqualTo(5);
        assertThat(MatchRules.wicketsForOvers(20)).isEqualTo(10);
        assertThrows(IllegalArgumentException.class, () -> MatchRules.wicketsForOvers(0));
        assertThrows(IllegalArgumentException.class,
                () -> new MatchEngine(MatchConfig.standard(1, 10, Difficulty.EASY), Collections.emptyList()));
    }
}
