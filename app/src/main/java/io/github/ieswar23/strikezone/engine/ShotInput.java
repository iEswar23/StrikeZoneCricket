package io.github.ieswar23.strikezone.engine;

/**
 * What the player did for one delivery: whether they swung, how far from the ideal moment
 * (negative = early, in ms) and in which direction.
 */
public final class ShotInput {
    private final boolean played;
    private final long offsetMs;
    private final ShotDirection direction;

    private ShotInput(boolean played, long offsetMs, ShotDirection direction) {
        this.played = played;
        this.offsetMs = offsetMs;
        this.direction = direction;
    }

    public static ShotInput swing(long offsetMs, ShotDirection direction) {
        return new ShotInput(true, offsetMs, direction == null ? ShotDirection.STRAIGHT : direction);
    }

    public static ShotInput leave() {
        return new ShotInput(false, 0L, ShotDirection.STRAIGHT);
    }

    public boolean isPlayed() {
        return played;
    }

    public long getOffsetMs() {
        return offsetMs;
    }

    public ShotDirection getDirection() {
        return direction;
    }
}
