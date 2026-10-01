package io.github.ieswar23.strikezone.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "challenges")
public class ChallengeEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    public int dayIndex;
    @NonNull
    public String title = "";
    public String description;
    public int overs;
    public int target;
    @NonNull
    public String difficulty = "MEDIUM";
    @NonNull
    public String opponentId = "";
    public String bowlerFocus;
    public String badge;
}
