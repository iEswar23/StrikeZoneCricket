package io.github.ieswar23.strikezone.engine;

/**
 * Short-format rules. A full innings has ten wickets, which would make 1–5 over chases trivial, so
 * the wicket allowance scales with the overs:
 * <ul>
 *   <li>1 over  → 2 wickets</li>
 *   <li>2 overs → 3 wickets</li>
 *   <li>3+ overs → one wicket per over, capped at 10 (5 overs → 5 wickets)</li>
 * </ul>
 */
public final class MatchRules {
    public static final int BALLS_PER_OVER = 6;
    public static final int MAX_WICKETS = 10;
    public static final int[] SUPPORTED_OVERS = {1, 2, 5};

    private MatchRules() {
    }

    public static int wicketsForOvers(int overs) {
        if (overs <= 0) throw new IllegalArgumentException("overs must be positive");
        if (overs == 1) return 2;
        if (overs == 2) return 3;
        return Math.min(MAX_WICKETS, overs);
    }
}
