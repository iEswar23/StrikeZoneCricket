package io.github.ieswar23.strikezone.domain.model;

import android.os.Bundle;

import androidx.lifecycle.SavedStateHandle;

import io.github.ieswar23.strikezone.engine.Difficulty;

/**
 * Everything needed to start a chase. Travels between screens as navigation arguments; the keys
 * match the argument names declared in {@code nav_graph.xml}.
 */
public final class MatchSetup {
    public static final String ARG_BATTING = "battingTeamId";
    public static final String ARG_BOWLING = "bowlingTeamId";
    public static final String ARG_OVERS = "overs";
    public static final String ARG_TARGET = "target";
    public static final String ARG_DIFFICULTY = "difficulty";
    public static final String ARG_MODE = "mode";
    public static final String ARG_CHALLENGE = "challengeId";
    public static final String ARG_BOWLER_FOCUS = "bowlerFocus";

    private final String battingTeamId;
    private final String bowlingTeamId;
    private final int overs;
    private final int target;
    private final Difficulty difficulty;
    private final GameMode mode;
    private final String challengeId;
    private final String bowlerFocus;

    public MatchSetup(String battingTeamId, String bowlingTeamId, int overs, int target, Difficulty difficulty,
                      GameMode mode, String challengeId, String bowlerFocus) {
        this.battingTeamId = battingTeamId;
        this.bowlingTeamId = bowlingTeamId;
        this.overs = overs;
        this.target = target;
        this.difficulty = difficulty;
        this.mode = mode;
        this.challengeId = challengeId;
        this.bowlerFocus = bowlerFocus;
    }

    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString(ARG_BATTING, battingTeamId);
        b.putString(ARG_BOWLING, bowlingTeamId);
        b.putInt(ARG_OVERS, overs);
        b.putInt(ARG_TARGET, target);
        b.putString(ARG_DIFFICULTY, difficulty.name());
        b.putString(ARG_MODE, mode.name());
        b.putString(ARG_CHALLENGE, challengeId);
        b.putString(ARG_BOWLER_FOCUS, bowlerFocus);
        return b;
    }

    public static MatchSetup fromSavedState(SavedStateHandle handle) {
        String batting = handle.get(ARG_BATTING);
        String bowling = handle.get(ARG_BOWLING);
        Integer overs = handle.get(ARG_OVERS);
        Integer target = handle.get(ARG_TARGET);
        String difficulty = handle.get(ARG_DIFFICULTY);
        String mode = handle.get(ARG_MODE);
        if (batting == null || bowling == null || overs == null || target == null) {
            throw new IllegalArgumentException("Missing match setup arguments");
        }
        return new MatchSetup(batting, bowling, overs, target, Difficulty.fromName(difficulty, Difficulty.MEDIUM),
                GameMode.fromName(mode), handle.get(ARG_CHALLENGE), handle.get(ARG_BOWLER_FOCUS));
    }

    public String getBattingTeamId() {
        return battingTeamId;
    }

    public String getBowlingTeamId() {
        return bowlingTeamId;
    }

    public int getOvers() {
        return overs;
    }

    public int getTarget() {
        return target;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public GameMode getMode() {
        return mode;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public String getBowlerFocus() {
        return bowlerFocus;
    }
}
