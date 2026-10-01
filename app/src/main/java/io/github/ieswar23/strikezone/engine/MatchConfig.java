package io.github.ieswar23.strikezone.engine;

/** Immutable parameters of a chase. */
public final class MatchConfig {
    private final int overs;
    private final int wicketsLimit;
    private final int target;
    private final Difficulty difficulty;

    public MatchConfig(int overs, int wicketsLimit, int target, Difficulty difficulty) {
        if (overs <= 0) throw new IllegalArgumentException("overs must be positive");
        if (wicketsLimit <= 0 || wicketsLimit > MatchRules.MAX_WICKETS) {
            throw new IllegalArgumentException("wicketsLimit must be in 1..10");
        }
        if (target <= 0) throw new IllegalArgumentException("target must be positive");
        this.overs = overs;
        this.wicketsLimit = wicketsLimit;
        this.target = target;
        this.difficulty = difficulty;
    }

    /** Config using the standard wicket allowance for the given overs. */
    public static MatchConfig standard(int overs, int target, Difficulty difficulty) {
        return new MatchConfig(overs, MatchRules.wicketsForOvers(overs), target, difficulty);
    }

    public int getOvers() {
        return overs;
    }

    public int getWicketsLimit() {
        return wicketsLimit;
    }

    public int getTarget() {
        return target;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getTotalBalls() {
        return overs * MatchRules.BALLS_PER_OVER;
    }
}
