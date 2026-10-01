package io.github.ieswar23.strikezone.engine;

/** Converts a tap offset into a {@link Timing} using difficulty- and bowler-dependent windows. */
public final class TimingJudge {

    private TimingJudge() {
    }

    public static long perfectWindowMs(Difficulty difficulty, BowlerType type) {
        return Math.round(difficulty.getPerfectWindowMs() * type.getWindowMultiplier());
    }

    public static long goodWindowMs(Difficulty difficulty, BowlerType type) {
        return Math.round(difficulty.getGoodWindowMs() * type.getWindowMultiplier());
    }

    /**
     * @param offsetMs tap time minus the moment the ball reaches the hitting zone; negative = early
     */
    public static Timing classify(long offsetMs, Difficulty difficulty, BowlerType type) {
        long abs = Math.abs(offsetMs);
        if (abs <= perfectWindowMs(difficulty, type)) return Timing.PERFECT;
        if (abs <= goodWindowMs(difficulty, type)) return offsetMs < 0 ? Timing.EARLY : Timing.LATE;
        return Timing.MISSED;
    }
}
