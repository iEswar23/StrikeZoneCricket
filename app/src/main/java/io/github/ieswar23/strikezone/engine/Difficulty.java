package io.github.ieswar23.strikezone.engine;

/**
 * Difficulty shrinks the timing windows, speeds the ball up and makes fielders sharper.
 */
public enum Difficulty {
    EASY("Easy", 75, 175, 1.15f, 0.8f),
    MEDIUM("Medium", 52, 135, 1.0f, 1.0f),
    HARD("Hard", 36, 100, 0.9f, 1.5f);

    private final String label;
    private final int perfectWindowMs;
    private final int goodWindowMs;
    private final float travelFactor;
    private final float catchFactor;

    Difficulty(String label, int perfectWindowMs, int goodWindowMs, float travelFactor, float catchFactor) {
        this.label = label;
        this.perfectWindowMs = perfectWindowMs;
        this.goodWindowMs = goodWindowMs;
        this.travelFactor = travelFactor;
        this.catchFactor = catchFactor;
    }

    public String getLabel() {
        return label;
    }

    /** Half-width of the PERFECT window, before the bowler-type multiplier. */
    public int getPerfectWindowMs() {
        return perfectWindowMs;
    }

    /** Half-width of the EARLY/LATE window, before the bowler-type multiplier. */
    public int getGoodWindowMs() {
        return goodWindowMs;
    }

    /** Multiplier applied to ball travel time (&gt; 1 means slower ball). */
    public float getTravelFactor() {
        return travelFactor;
    }

    /** Multiplier applied to catching chances in {@link ShotResolver}. */
    public float getCatchFactor() {
        return catchFactor;
    }

    public static Difficulty fromName(String name, Difficulty fallback) {
        if (name == null) return fallback;
        for (Difficulty d : values()) {
            if (d.name().equalsIgnoreCase(name)) return d;
        }
        return fallback;
    }
}
