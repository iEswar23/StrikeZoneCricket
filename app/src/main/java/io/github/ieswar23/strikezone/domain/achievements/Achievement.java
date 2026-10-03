package io.github.ieswar23.strikezone.domain.achievements;

/**
 * The badges a player can unlock. Each one has a numeric target: single-match feats (a fifty,
 * three sixes in an over) track the best effort so far, career feats (wins, sixes) track the
 * running total. Yes/no feats have a target of 1.
 *
 * <p>The declaration order is the order the achievements screen lists them in.
 */
public enum Achievement {
    FIRST_WIN("First Victory", "Win your first chase", 1),
    FIRST_FIFTY("First Fifty", "Score 50 runs off the bat in one chase", 50),
    SIX_MACHINE("Six Machine", "Hit 3 sixes in a single over", 3),
    PERFECT_TEN("Perfect Timing ×10", "Time 10 shots perfectly in one chase", 10),
    DOT_FREE_OVER("No Dot Ball Over", "Score off all six balls of an over", 1),
    FLAWLESS_CHASE("Flawless Chase", "Win a chase without losing a wicket", 1),
    LAST_BALL_HERO("Last-Ball Hero", "Win a chase off the very last ball", 1),
    HARD_MODE_WINNER("Hard Mode Winner", "Win a chase on Hard difficulty", 1),
    CHASE_MASTER("Chase Master", "Win 5 chases", 5),
    DAILY_CHALLENGER("Daily Challenger", "Win the Daily Challenge 3 days in a row", 3),
    BIG_HITTER("Big Hitter", "Hit 50 sixes in your career", 50),
    BOUNDARY_CENTURY("Boundary Century", "Hit 100 career boundaries (fours and sixes)", 100);

    private final String title;
    private final String description;
    private final int target;

    Achievement(String title, String description, int target) {
        this.title = title;
        this.description = description;
        this.target = target;
    }

    public String getTitle() {
        return title;
    }

    /** What the player has to do; shown as the hint on a locked badge. */
    public String getDescription() {
        return description;
    }

    /** Value of the tracked number at which the badge unlocks. */
    public int getTarget() {
        return target;
    }

    /** True for yes/no feats, which show no "x/y" counter. */
    public boolean isOneOff() {
        return target == 1;
    }

    /** Looks an achievement up by its persisted id, or returns null for an unknown (removed) one. */
    public static Achievement fromId(String id) {
        if (id == null) return null;
        for (Achievement a : values()) {
            if (a.name().equals(id)) return a;
        }
        return null;
    }
}
