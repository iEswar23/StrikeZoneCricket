package io.github.ieswar23.strikezone.engine;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Random;

public class ScoreCalculatorTest {

    @Test
    public void currentRunRateIsRunsPerSixBalls() {
        assertThat(ScoreCalculator.runRate(23, 14)).isWithin(1e-9).of(23 * 6 / 14.0);
        assertThat(ScoreCalculator.formatRate(ScoreCalculator.runRate(23, 14))).isEqualTo("9.86");
        assertThat(ScoreCalculator.runRate(10, 0)).isEqualTo(0.0);
    }

    @Test
    public void requiredRunRateHandlesEdgeCases() {
        assertThat(ScoreCalculator.requiredRunRate(12, 9)).isWithin(1e-9).of(8.0);
        assertThat(ScoreCalculator.requiredRunRate(0, 9)).isEqualTo(0.0);
        assertThat(ScoreCalculator.requiredRunRate(5, 0)).isPositiveInfinity();
        assertThat(ScoreCalculator.formatRate(Double.POSITIVE_INFINITY)).isEqualTo("—");
    }

    @Test
    public void oversAreFormattedInCricketNotation() {
        assertThat(ScoreCalculator.formatOvers(0)).isEqualTo("0.0");
        assertThat(ScoreCalculator.formatOvers(5)).isEqualTo("0.5");
        assertThat(ScoreCalculator.formatOvers(23)).isEqualTo("3.5");
        assertThat(ScoreCalculator.formatOvers(30)).isEqualTo("5.0");
    }

    @Test
    public void strikeRateIsRunsPerHundredBalls() {
        assertThat(ScoreCalculator.strikeRate(30, 20)).isWithin(1e-9).of(150.0);
        assertThat(ScoreCalculator.formatStrikeRate(7, 3)).isEqualTo("233.3");
        assertThat(ScoreCalculator.strikeRate(5, 0)).isEqualTo(0.0);
    }

    @Test
    public void winningResultTextCountsWicketsInHandAndBallsToSpare() {
        assertThat(ScoreCalculator.resultText(MatchStatus.WON, "Mumbai Mavericks", "London Lions", 25, 1, 3, 24, 5))
                .isEqualTo("Mumbai Mavericks won by 2 wickets (5 balls to spare)");
        assertThat(ScoreCalculator.resultText(MatchStatus.WON, "Mumbai Mavericks", "London Lions", 19, 1, 2, 19, 1))
                .isEqualTo("Mumbai Mavericks won by 1 wicket (1 ball to spare)");
        assertThat(ScoreCalculator.resultText(MatchStatus.WON, "Sydney Strikers", "Colombo Cyclones", 30, 0, 3, 28, 0))
                .isEqualTo("Sydney Strikers won by 3 wickets");
    }

    @Test
    public void losingResultTextCreditsTheDefendingSideWithTheRunMargin() {
        assertThat(ScoreCalculator.resultText(MatchStatus.LOST, "Chennai Chargers", "London Lions", 37, 3, 3, 50, 2))
                .isEqualTo("London Lions won by 12 runs");
        assertThat(ScoreCalculator.resultText(MatchStatus.LOST, "Chennai Chargers", "London Lions", 48, 5, 5, 50, 0))
                .isEqualTo("London Lions won by 1 run");
        assertThat(ScoreCalculator.resultText(MatchStatus.TIED, "A", "B", 49, 2, 5, 50, 0))
                .isEqualTo("Match tied! Scores level on 49");
    }

    @Test
    public void chaseEquationAndWinPercentage() {
        assertThat(ScoreCalculator.chaseEquation(1, 1)).isEqualTo("Need 1 run from 1 ball");
        assertThat(ScoreCalculator.chaseEquation(16, 8)).isEqualTo("Need 16 runs from 8 balls");
        assertThat(ScoreCalculator.chaseEquation(0, 3)).isEqualTo("Target reached");
        assertThat(ScoreCalculator.winPercentage(2, 3)).isEqualTo(67);
        assertThat(ScoreCalculator.winPercentage(0, 0)).isEqualTo(0);
    }

    @Test
    public void quickChaseTargetsStayWithinTheDifficultyBand() {
        Random random = new Random(11);
        for (int i = 0; i < 200; i++) {
            for (int overs : MatchRules.SUPPORTED_OVERS) {
                int easy = TargetGenerator.quickChaseTarget(overs, Difficulty.EASY, random);
                int hard = TargetGenerator.quickChaseTarget(overs, Difficulty.HARD, random);
                assertThat(easy).isIn(com.google.common.collect.Range.closed(overs * 8 + 1, overs * 11 + 1));
                assertThat(hard).isIn(com.google.common.collect.Range.closed(overs * 12 + 1, overs * 15 + 1));
            }
        }
    }
}
