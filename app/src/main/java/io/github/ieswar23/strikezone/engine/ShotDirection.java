package io.github.ieswar23.strikezone.engine;

/**
 * Where the batter aims the shot. The camera sits behind a right-handed batter, so the leg side
 * is on the left of the screen and the off side is on the right.
 */
public enum ShotDirection {
    LEG("Leg side", -55.0),
    STRAIGHT("Straight", 0.0),
    OFF("Off side", 55.0);

    private final String label;
    private final double centreAngleDeg;

    ShotDirection(String label, double centreAngleDeg) {
        this.label = label;
        this.centreAngleDeg = centreAngleDeg;
    }

    public String getLabel() {
        return label;
    }

    /** Angle of the middle of this scoring zone; 0 = straight down the ground, positive = off side. */
    public double getCentreAngleDeg() {
        return centreAngleDeg;
    }
}
