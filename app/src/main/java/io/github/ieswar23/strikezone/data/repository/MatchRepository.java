package io.github.ieswar23.strikezone.data.repository;

import androidx.lifecycle.LiveData;

import java.util.List;

import io.github.ieswar23.strikezone.data.local.MatchStats;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;

/** Completed matches, career stats and best scores. */
public interface MatchRepository {

    interface SaveCallback {
        void onSaved(long matchId);
    }

    LiveData<List<MatchEntity>> observeHistory();

    LiveData<MatchEntity> observeMatch(long id);

    LiveData<List<MatchEntity>> observeBestScores(int limit);

    LiveData<MatchStats> observeStats();

    LiveData<Integer> observeChallengeWins(String challengeId);

    /** Inserts in the background and reports the new id on the main thread. */
    void save(MatchEntity match, SaveCallback callback);

    void delete(MatchEntity match);

    /** Re-inserts a previously deleted match (snackbar undo). */
    void restore(MatchEntity match);

    void clearAll();
}
