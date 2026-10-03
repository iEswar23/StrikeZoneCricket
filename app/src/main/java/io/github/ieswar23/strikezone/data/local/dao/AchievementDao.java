package io.github.ieswar23.strikezone.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.github.ieswar23.strikezone.data.local.entity.AchievementEntity;

@Dao
public interface AchievementDao {
    /** Keeps the first unlock time if a badge is somehow inserted twice. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<AchievementEntity> achievements);

    @Query("SELECT * FROM achievements ORDER BY unlockedAt ASC")
    LiveData<List<AchievementEntity>> observeAll();

    @Query("SELECT * FROM achievements WHERE matchId = :matchId ORDER BY unlockedAt ASC")
    LiveData<List<AchievementEntity>> observeUnlockedBy(long matchId);

    /** Blocking snapshot; call from a background thread. */
    @Query("SELECT * FROM achievements")
    List<AchievementEntity> getAllSync();

    @Query("DELETE FROM achievements")
    void deleteAll();
}
