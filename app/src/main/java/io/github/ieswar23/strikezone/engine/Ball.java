package io.github.ieswar23.strikezone.engine;

/** A ball that has been recorded in an over. */
public final class Ball {
    private final int overIndex;
    private final int sequence;
    private final OutcomeType type;
    private final String description;

    public Ball(int overIndex, int sequence, OutcomeType type, String description) {
        this.overIndex = overIndex;
        this.sequence = sequence;
        this.type = type;
        this.description = description;
    }

    /** Zero-based over the ball belongs to. */
    public int getOverIndex() {
        return overIndex;
    }

    /** Position within the over, counting wides (1-based). */
    public int getSequence() {
        return sequence;
    }

    public OutcomeType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public int getRuns() {
        return type.getRuns();
    }

    public boolean isLegal() {
        return type.isLegal();
    }

    public boolean isWicket() {
        return type.isWicket();
    }
}
