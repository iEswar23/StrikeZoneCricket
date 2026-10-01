package io.github.ieswar23.strikezone.engine;

/** How well the swing was timed relative to the ball reaching the hitting zone. */
public enum Timing {
    PERFECT("Perfect"),
    EARLY("Early"),
    LATE("Late"),
    /** Swung, but outside the playable window. */
    MISSED("Missed"),
    /** Did not offer a shot at all. */
    LEFT_ALONE("Left");

    private final String label;

    Timing(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean madeContact() {
        return this == PERFECT || this == EARLY || this == LATE;
    }
}
