package io.github.ieswar23.strikezone.engine;

/**
 * Length of a delivery. {@link #getBounceFraction()} is where the ball pitches, as a fraction of
 * the distance from the bowler's crease (0) to the batter's crease (1).
 */
public enum Length {
    YORKER(0.95f, 0.05f),
    FULL(0.80f, 0.35f),
    GOOD(0.66f, 0.60f),
    SHORT(0.48f, 1.00f);

    private final float bounceFraction;
    private final float riseAfterBounce;

    Length(float bounceFraction, float riseAfterBounce) {
        this.bounceFraction = bounceFraction;
        this.riseAfterBounce = riseAfterBounce;
    }

    public float getBounceFraction() {
        return bounceFraction;
    }

    /** Relative height of the ball when it reaches the batter (0 = along the ground, 1 = chest). */
    public float getRiseAfterBounce() {
        return riseAfterBounce;
    }
}
