package io.github.ieswar23.strikezone.engine;

import java.util.Locale;

/** Cricket arithmetic and scorecard formatting. Stateless. */
public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    /** Runs per over. Returns 0 before the first legal ball. */
    public static double runRate(int runs, int legalBalls) {
        if (legalBalls <= 0) return 0.0;
        return runs * (double) MatchRules.BALLS_PER_OVER / legalBalls;
    }

    /**
     * Runs per over still needed. Returns 0 when nothing is needed and positive infinity when runs
     * are needed but no balls remain.
     */
    public static double requiredRunRate(int runsNeeded, int ballsRemaining) {
        if (runsNeeded <= 0) return 0.0;
        if (ballsRemaining <= 0) return Double.POSITIVE_INFINITY;
        return runsNeeded * (double) MatchRules.BALLS_PER_OVER / ballsRemaining;
    }

    public static String formatRate(double rate) {
        if (Double.isInfinite(rate) || Double.isNaN(rate)) return "—";
        return String.format(Locale.US, "%.2f", rate);
    }

    /** Cricket overs notation: 23 legal balls → "3.5". */
    public static String formatOvers(int legalBalls) {
        int b = Math.max(0, legalBalls);
        return (b / MatchRules.BALLS_PER_OVER) + "." + (b % MatchRules.BALLS_PER_OVER);
    }

    /** Runs per 100 balls. */
    public static double strikeRate(int runs, int balls) {
        if (balls <= 0) return 0.0;
        return runs * 100.0 / balls;
    }

    public static String formatStrikeRate(int runs, int balls) {
        return String.format(Locale.US, "%.1f", strikeRate(runs, balls));
    }

    public static int winPercentage(int wins, int matches) {
        if (matches <= 0) return 0;
        return (int) Math.round(wins * 100.0 / matches);
    }

    /** "Need 12 runs from 9 balls" style equation for the HUD. */
    public static String chaseEquation(int runsNeeded, int ballsRemaining) {
        if (runsNeeded <= 0) return "Target reached";
        return "Need " + plural(runsNeeded, "run") + " from " + plural(ballsRemaining, "ball");
    }

    /**
     * Result line from the chasing side's point of view, e.g.
     * "Mumbai Mavericks won by 2 wickets (5 balls to spare)" or "London Lions won by 12 runs".
     */
    public static String resultText(MatchStatus status, String battingTeam, String bowlingTeam,
                                    int runs, int wickets, int wicketsLimit, int target, int ballsRemaining) {
        switch (status) {
            case WON: {
                int wicketsInHand = Math.max(1, wicketsLimit - wickets);
                String text = battingTeam + " won by " + plural(wicketsInHand, "wicket");
                if (ballsRemaining > 0) text += " (" + plural(ballsRemaining, "ball") + " to spare)";
                return text;
            }
            case LOST: {
                int margin = Math.max(1, target - 1 - runs);
                return bowlingTeam + " won by " + plural(margin, "run");
            }
            case TIED:
                return "Match tied! Scores level on " + runs;
            case IN_PROGRESS:
            default:
                return chaseEquation(target - runs, ballsRemaining);
        }
    }

    public static String resultText(MatchEngine engine, String battingTeam, String bowlingTeam) {
        MatchConfig c = engine.getConfig();
        return resultText(engine.getStatus(), battingTeam, bowlingTeam, engine.getRuns(), engine.getWickets(),
                c.getWicketsLimit(), c.getTarget(), engine.getBallsRemaining());
    }

    static String plural(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }
}
