package io.github.ieswar23.strikezone.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** An unlocked badge. Rows are only ever added, so a badge survives deleting the match that earned it. */
@Entity(tableName = "achievements", indices = {@Index("matchId")})
public class AchievementEntity {
    /** {@code Achievement#name()}. */
    @PrimaryKey
    @NonNull
    public String id = "";
    public long unlockedAt;
    /** The match that unlocked it, or 0 when it was back-filled from older history. */
    public long matchId;

    public AchievementEntity() {
    }

    @Ignore
    public AchievementEntity(@NonNull String id, long unlockedAt, long matchId) {
        this.id = id;
        this.unlockedAt = unlockedAt;
        this.matchId = matchId;
    }
}
