package io.github.ieswar23.strikezone.engine;

/** Result of a single ball. Run-outs credit the one run completed before the direct hit. */
public enum OutcomeType {
    DOT(0, false, true, "•"),
    ONE(1, false, true, "1"),
    TWO(2, false, true, "2"),
    THREE(3, false, true, "3"),
    FOUR(4, false, true, "4"),
    SIX(6, false, true, "6"),
    WIDE(1, false, false, "Wd"),
    CAUGHT(0, true, true, "W"),
    BOWLED(0, true, true, "W"),
    RUN_OUT(1, true, true, "W");

    private final int runs;
    private final boolean wicket;
    private final boolean legal;
    private final String chip;

    OutcomeType(int runs, boolean wicket, boolean legal, String chip) {
        this.runs = runs;
        this.wicket = wicket;
        this.legal = legal;
        this.chip = chip;
    }

    public int getRuns() {
        return runs;
    }

    public boolean isWicket() {
        return wicket;
    }

    /** Wides are not legal deliveries and do not count towards the six balls of an over. */
    public boolean isLegal() {
        return legal;
    }

    public boolean isBoundary() {
        return this == FOUR || this == SIX;
    }

    /** Short label used in the "this over" ball chips. */
    public String getChip() {
        return chip;
    }

    /** Runs credited to the batter (wides are extras). */
    public int getBatRuns() {
        return this == WIDE ? 0 : runs;
    }

    public static OutcomeType forRuns(int runs) {
        switch (runs) {
            case 0: return DOT;
            case 1: return ONE;
            case 2: return TWO;
            case 3: return THREE;
            case 4: return FOUR;
            case 6: return SIX;
            default: throw new IllegalArgumentException("Unsupported run value: " + runs);
        }
    }
}
