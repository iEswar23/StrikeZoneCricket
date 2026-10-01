package io.github.ieswar23.strikezone.domain.model;

import java.util.List;

import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.engine.MatchRules;

/** A curated chase served by the challenges API. One rotates in each calendar day. */
public final class DailyChallenge {
    private final String id;
    private final int dayIndex;
    private final String title;
    private final String description;
    private final int overs;
    private final int target;
    private final Difficulty difficulty;
    private final String opponentId;
    private final String bowlerFocus;
    private final String badge;

    public DailyChallenge(String id, int dayIndex, String title, String description, int overs, int target,
                          Difficulty difficulty, String opponentId, String bowlerFocus, String badge) {
        this.id = id;
        this.dayIndex = dayIndex;
        this.title = title;
        this.description = description;
        this.overs = overs;
        this.target = target;
        this.difficulty = difficulty;
        this.opponentId = opponentId;
        this.bowlerFocus = bowlerFocus;
        this.badge = badge;
    }

    /** Picks the challenge for a given day of the year from a rotation ordered by dayIndex. */
    public static DailyChallenge forDay(List<DailyChallenge> rotation, int dayOfYear) {
        if (rotation == null || rotation.isEmpty()) return null;
        return rotation.get(Math.floorMod(dayOfYear, rotation.size()));
    }

    public String getId() {
        return id;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getOvers() {
        return overs;
    }

    public int getTarget() {
        return target;
    }

    public int getWickets() {
        return MatchRules.wicketsForOvers(overs);
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getOpponentId() {
        return opponentId;
    }

    /** PACE, SPIN or MIXED: restricts which of the opponent's bowlers are used. */
    public String getBowlerFocus() {
        return bowlerFocus;
    }

    public String getBadge() {
        return badge;
    }
}
