package io.github.ieswar23.strikezone.engine;

/** A named bowler from the fielding side. */
public final class Bowler {
    private final String name;
    private final BowlerType type;

    public Bowler(String name, BowlerType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public BowlerType getType() {
        return type;
    }
}
