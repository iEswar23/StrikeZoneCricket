package io.github.ieswar23.strikezone.ui.game;

import java.util.Collections;
import java.util.List;

import io.github.ieswar23.strikezone.engine.MatchStatus;
import io.github.ieswar23.strikezone.engine.OutcomeType;

/** Immutable snapshot of the scoreboard HUD. */
public final class GameUiState {
    public final boolean loading;
    public final String battingName;
    public final String battingShort;
    public final String battingColor;
    public final String bowlingName;
    public final String bowlingShort;
    public final String bowlingColor;
    public final int runs;
    public final int wickets;
    public final int wicketsLimit;
    public final int legalBalls;
    public final int overs;
    public final int target;
    public final String oversText;
    public final String equationText;
    public final String currentRunRate;
    public final String requiredRunRate;
    public final List<OutcomeType> thisOver;
    public final String bowlerName;
    public final String bowlerType;
    public final String fieldPlan;
    public final String modeLabel;
    public final MatchStatus status;
    public final String resultText;

    public GameUiState(boolean loading, String battingName, String battingShort, String battingColor,
                       String bowlingName, String bowlingShort, String bowlingColor, int runs, int wickets,
                       int wicketsLimit, int legalBalls, int overs, int target, String oversText,
                       String equationText, String currentRunRate, String requiredRunRate,
                       List<OutcomeType> thisOver, String bowlerName, String bowlerType, String fieldPlan,
                       String modeLabel, MatchStatus status, String resultText) {
        this.loading = loading;
        this.battingName = battingName;
        this.battingShort = battingShort;
        this.battingColor = battingColor;
        this.bowlingName = bowlingName;
        this.bowlingShort = bowlingShort;
        this.bowlingColor = bowlingColor;
        this.runs = runs;
        this.wickets = wickets;
        this.wicketsLimit = wicketsLimit;
        this.legalBalls = legalBalls;
        this.overs = overs;
        this.target = target;
        this.oversText = oversText;
        this.equationText = equationText;
        this.currentRunRate = currentRunRate;
        this.requiredRunRate = requiredRunRate;
        this.thisOver = thisOver == null ? Collections.emptyList() : Collections.unmodifiableList(thisOver);
        this.bowlerName = bowlerName;
        this.bowlerType = bowlerType;
        this.fieldPlan = fieldPlan;
        this.modeLabel = modeLabel;
        this.status = status;
        this.resultText = resultText;
    }

    public static GameUiState loading() {
        return new GameUiState(true, "", "", "#1E6FD9", "", "", "#C8102E", 0, 0, 1, 0, 1, 1, "0.0", "", "0.00",
                "0.00", null, "", "", "", "", MatchStatus.IN_PROGRESS, "");
    }

    public String scoreText() {
        return runs + "/" + wickets;
    }
}
