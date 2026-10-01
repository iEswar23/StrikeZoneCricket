package io.github.ieswar23.strikezone.engine;

/**
 * A fielding position expressed in polar coordinates around the batter: angle 0 is straight down
 * the ground, positive angles are on the off side, |angle| &gt; 90 is behind square. Depth is a
 * fraction of the distance to the boundary in that direction.
 */
public final class Fielder {
    private final String position;
    private final ShotDirection zone;
    private final boolean deep;
    private final double angleDeg;
    private final double depth;

    public Fielder(String position, ShotDirection zone, boolean deep, double angleDeg, double depth) {
        this.position = position;
        this.zone = zone;
        this.deep = deep;
        this.angleDeg = angleDeg;
        this.depth = depth;
    }

    public String getPosition() {
        return position;
    }

    /** The scoring zone this fielder protects, or {@code null} for positions behind square. */
    public ShotDirection getZone() {
        return zone;
    }

    public boolean isDeep() {
        return deep;
    }

    public double getAngleDeg() {
        return angleDeg;
    }

    public double getDepth() {
        return depth;
    }
}
