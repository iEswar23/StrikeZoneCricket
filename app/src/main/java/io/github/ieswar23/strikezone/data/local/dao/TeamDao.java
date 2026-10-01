package io.github.ieswar23.strikezone.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import io.github.ieswar23.strikezone.data.local.entity.TeamEntity;

@Dao
public interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY rating DESC, name ASC")
    LiveData<List<TeamEntity>> observeAll();

    @Query("SELECT * FROM teams WHERE id = :id LIMIT 1")
    TeamEntity getById(String id);

    @Query("SELECT COUNT(*) FROM teams")
    int count();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<TeamEntity> teams);
}
