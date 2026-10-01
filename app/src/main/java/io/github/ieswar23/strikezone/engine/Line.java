package io.github.ieswar23.strikezone.engine;

/**
 * Line of a delivery. {@link #getOffset()} is the lateral position at the batter in pitch
 * half-widths (positive = off side / screen right).
 */
public enum Line {
    WIDE(1.05f, ShotDirection.OFF),
    OUTSIDE_OFF(0.48f, ShotDirection.OFF),
    ON_STUMPS(0.0f, ShotDirection.STRAIGHT),
    ON_PADS(-0.36f, ShotDirection.LEG);

    private final float offset;
    private final ShotDirection naturalDirection;

    Line(float offset, ShotDirection naturalDirection) {
        this.offset = offset;
        this.naturalDirection = naturalDirection;
    }

    public float getOffset() {
        return offset;
    }

    /** The direction a well-played shot to this line naturally goes. */
    public ShotDirection getNaturalDirection() {
        return naturalDirection;
    }
}
