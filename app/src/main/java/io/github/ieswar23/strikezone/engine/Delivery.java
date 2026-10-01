package io.github.ieswar23.strikezone.engine;

/** One ball as bowled: immutable description of speed, line, length and movement. */
public final class Delivery {
    private final BowlerType bowlerType;
    private final int speedKph;
    private final long travelMs;
    private final Line line;
    private final Length length;
    private final float deviation;

    public Delivery(BowlerType bowlerType, int speedKph, long travelMs, Line line, Length length, float deviation) {
        if (travelMs <= 0) throw new IllegalArgumentException("travelMs must be positive");
        this.bowlerType = bowlerType;
        this.speedKph = speedKph;
        this.travelMs = travelMs;
        this.line = line;
        this.length = length;
        this.deviation = deviation;
    }

    public BowlerType getBowlerType() {
        return bowlerType;
    }

    public int getSpeedKph() {
        return speedKph;
    }

    /** Time from release to the ball reaching the batter's hitting zone. */
    public long getTravelMs() {
        return travelMs;
    }

    public Line getLine() {
        return line;
    }

    public Length getLength() {
        return length;
    }

    /** Sideways movement after the bounce, in pitch half-widths (positive = towards off side). */
    public float getDeviation() {
        return deviation;
    }

    public boolean isWide() {
        return line == Line.WIDE;
    }

    /** True when the ball would go on to hit the stumps if the batter missed it. */
    public boolean isHittingStumps() {
        return line == Line.ON_STUMPS && length != Length.SHORT && Math.abs(deviation) < 0.3f;
    }

    @Override
    public String toString() {
        return bowlerType.getLabel() + " " + speedKph + "kph " + length + " " + line;
    }
}
