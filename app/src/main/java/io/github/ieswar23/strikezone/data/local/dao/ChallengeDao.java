package io.github.ieswar23.strikezone.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.github.ieswar23.strikezone.data.local.entity.ChallengeEntity;

@Dao
public interface ChallengeDao {
    @Query("SELECT * FROM challenges ORDER BY dayIndex ASC")
    LiveData<List<ChallengeEntity>> observeAll();

    @Query("SELECT COUNT(*) FROM challenges")
    int count();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<ChallengeEntity> challenges);
}
