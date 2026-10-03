package io.github.ieswar23.strikezone.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A completed chase. Denormalised so history survives a team catalogue refresh. */
@Entity(tableName = "matches", indices = {@Index("playedAt"), @Index("challengeId")})
public class MatchEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long playedAt;
    @NonNull
    public String mode = "QUICK_CHASE";
    public String challengeId;
    public String bowlerFocus;

    @NonNull
    public String battingTeamId = "";
    @NonNull
    public String battingTeamName = "";
    @NonNull
    public String battingShort = "";
    @NonNull
    public String battingColor = "#1E6FD9";
    @NonNull
    public String bowlingTeamId = "";
    @NonNull
    public String bowlingTeamName = "";
    @NonNull
    public String bowlingShort = "";
    @NonNull
    public String bowlingColor = "#C8102E";

    public int overs;
    public int wicketsLimit;
    public int target;
    @NonNull
    public String difficulty = "MEDIUM";

    public int runs;
    public int wickets;
    /** Legal balls faced. */
    public int balls;
    public int fours;
    public int sixes;
    public int dots;
    public int extras;
    /** Shots timed inside the perfect window (added in schema v2). */
    @ColumnInfo(defaultValue = "0")
    public int perfectShots;
    /** Most sixes in any single over (added in schema v2). */
    @ColumnInfo(defaultValue = "0")
    public int maxSixesInOver;
    /** Completed overs scored off every legal ball (added in schema v2). */
    @ColumnInfo(defaultValue = "0")
    public int scoringOvers;
    public boolean won;
    public boolean tied;
    @NonNull
    public String resultText = "";
    /** Per-over "runs:wickets" pairs separated by commas, e.g. "8:0,14:1". */
    @NonNull
    public String overSummary = "";
}
