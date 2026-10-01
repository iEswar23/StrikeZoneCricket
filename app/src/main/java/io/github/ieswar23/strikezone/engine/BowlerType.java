package io.github.ieswar23.strikezone.engine;

/**
 * Bowling styles. Each style has a speed band (km/h), a maximum lateral deviation after the
 * bounce (in pitch half-widths) and a timing-window multiplier: slower bowling is more forgiving.
 */
public enum BowlerType {
    PACE("Pace", 132, 152, 0.12f, 0.90f, 1050L),
    MEDIUM("Medium", 114, 131, 0.20f, 1.00f, 850L),
    SPIN("Spin", 78, 96, 0.38f, 1.15f, 560L);

    private final String label;
    private final int minKph;
    private final int maxKph;
    private final float maxDeviation;
    private final float windowMultiplier;
    private final long runUpMs;

    BowlerType(String label, int minKph, int maxKph, float maxDeviation, float windowMultiplier, long runUpMs) {
        this.label = label;
        this.minKph = minKph;
        this.maxKph = maxKph;
        this.maxDeviation = maxDeviation;
        this.windowMultiplier = windowMultiplier;
        this.runUpMs = runUpMs;
    }

    public String getLabel() {
        return label;
    }

    public int getMinKph() {
        return minKph;
    }

    public int getMaxKph() {
        return maxKph;
    }

    public float getMaxDeviation() {
        return maxDeviation;
    }

    public float getWindowMultiplier() {
        return windowMultiplier;
    }

    /** Length of the bowler's run-up animation before release. */
    public long getRunUpMs() {
        return runUpMs;
    }

    public static BowlerType fromName(String name, BowlerType fallback) {
        if (name == null) return fallback;
        for (BowlerType type : values()) {
            if (type.name().equalsIgnoreCase(name)) return type;
        }
        return fallback;
    }
}
