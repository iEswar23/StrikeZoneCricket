package io.github.ieswar23.strikezone.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import io.github.ieswar23.strikezone.data.local.MatchStats;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;

@Dao
public interface MatchDao {
    @Insert
    long insert(MatchEntity match);

    @Delete
    void delete(MatchEntity match);

    @Query("DELETE FROM matches")
    void deleteAll();

    @Query("SELECT * FROM matches ORDER BY playedAt DESC")
    LiveData<List<MatchEntity>> observeAll();

    /** Blocking snapshot of the whole history; call from a background thread. */
    @Query("SELECT * FROM matches ORDER BY playedAt ASC")
    List<MatchEntity> getAllSync();

    @Query("SELECT * FROM matches WHERE id = :id LIMIT 1")
    LiveData<MatchEntity> observeById(long id);

    @Query("SELECT * FROM matches ORDER BY runs DESC, balls ASC, playedAt DESC LIMIT :limit")
    LiveData<List<MatchEntity>> observeBestScores(int limit);

    @Query("SELECT COUNT(*) FROM matches WHERE challengeId = :challengeId AND won = 1")
    LiveData<Integer> observeChallengeWins(String challengeId);

    @Query("SELECT COUNT(*) AS totalMatches, "
            + "COALESCE(SUM(CASE WHEN won = 1 THEN 1 ELSE 0 END), 0) AS wins, "
            + "COALESCE(SUM(CASE WHEN tied = 1 THEN 1 ELSE 0 END), 0) AS ties, "
            + "COALESCE(MAX(runs), 0) AS highestScore, "
            + "COALESCE(SUM(runs - extras), 0) AS totalRuns, "
            + "COALESCE(SUM(balls), 0) AS totalBalls, "
            + "COALESCE(SUM(fours), 0) AS totalFours, "
            + "COALESCE(SUM(sixes), 0) AS totalSixes, "
            + "COALESCE(SUM(CASE WHEN won = 1 AND challengeId IS NOT NULL THEN 1 ELSE 0 END), 0) AS challengesWon "
            + "FROM matches")
    LiveData<MatchStats> observeStats();
}
