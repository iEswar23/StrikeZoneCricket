package io.github.ieswar23.strikezone.data.repository;

import androidx.lifecycle.LiveData;

import java.util.List;

import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.util.SyncState;

/** Teams and daily challenges: fetched from the tournament API, cached in Room. */
public interface CricketRepository {
    LiveData<List<Team>> observeTeams();

    LiveData<List<DailyChallenge>> observeChallenges();

    LiveData<SyncState> syncState();

    /** Fetches from the API when the cache is empty, or always when {@code force} is true. */
    void refresh(boolean force);

    /** Blocking lookup; call from a background thread. */
    Team getTeamSync(String id);
}
