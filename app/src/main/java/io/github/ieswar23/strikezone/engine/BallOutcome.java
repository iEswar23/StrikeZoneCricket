package io.github.ieswar23.strikezone.engine;

/**
 * The resolved result of a delivery plus everything the UI needs to animate it: the timing, the
 * direction of the ball after the shot and the fielder involved (if any).
 */
public final class BallOutcome {
    private final OutcomeType type;
    private final Timing timing;
    private final ShotDirection direction;
    private final double shotAngleDeg;
    private final String fielderPosition;
    private final String headline;
    private final String description;

    public BallOutcome(OutcomeType type, Timing timing, ShotDirection direction, double shotAngleDeg,
                       String fielderPosition, String headline, String description) {
        this.type = type;
        this.timing = timing;
        this.direction = direction;
        this.shotAngleDeg = shotAngleDeg;
        this.fielderPosition = fielderPosition;
        this.headline = headline;
        this.description = description;
    }

    public OutcomeType getType() {
        return type;
    }

    public Timing getTiming() {
        return timing;
    }

    public ShotDirection getDirection() {
        return direction;
    }

    /** Angle the ball travels after the shot (0 = straight, positive = off side). */
    public double getShotAngleDeg() {
        return shotAngleDeg;
    }

    /** Fielder who caught / stopped / threw the ball, or null. */
    public String getFielderPosition() {
        return fielderPosition;
    }

    /** Big popup text, e.g. "SIX!" or "OUT!". */
    public String getHeadline() {
        return headline;
    }

    /** Commentary line, e.g. "Caught at long-on". */
    public String getDescription() {
        return description;
    }

    public int getRuns() {
        return type.getRuns();
    }

    public boolean isWicket() {
        return type.isWicket();
    }

    @Override
    public String toString() {
        return type + " (" + timing + ", " + direction + "): " + description;
    }
}
