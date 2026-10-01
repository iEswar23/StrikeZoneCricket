package io.github.ieswar23.strikezone.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.ieswar23.strikezone.data.local.MatchStats;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.data.repository.CricketRepository;
import io.github.ieswar23.strikezone.data.repository.MatchRepository;
import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.engine.Bowler;
import io.github.ieswar23.strikezone.engine.BowlerType;
import io.github.ieswar23.strikezone.util.SyncState;

/** In-memory fakes for ViewModel tests. */
public final class FakeRepositories {

    private FakeRepositories() {
    }

    public static final Team MUMBAI = new Team("mum", "Mumbai Mavericks", "MUM", "Mumbai", "#1E6FD9", "#FFD23F",
            "Arjun Deshpande", "Marine Drive Oval", 88, Arrays.asList(
            new Bowler("Rohan Kulkarni", BowlerType.PACE),
            new Bowler("Siddharth Rane", BowlerType.SPIN)));

    public static final Team COLOMBO = new Team("col", "Colombo Cyclones", "COL", "Colombo", "#0F4C81", "#5FD068",
            "Dhananjaya Perera", "Galle Face Grounds", 82, Arrays.asList(
            new Bowler("Nuwan Jayasinghe", BowlerType.SPIN),
            new Bowler("Lahiru Fernando", BowlerType.PACE),
            new Bowler("Kasun Wickramasinghe", BowlerType.MEDIUM),
            new Bowler("Chamika Silva", BowlerType.SPIN)));

    public static class FakeCricketRepository implements CricketRepository {
        public final MutableLiveData<List<Team>> teams = new MutableLiveData<>(Arrays.asList(MUMBAI, COLOMBO));
        public final MutableLiveData<List<DailyChallenge>> challenges = new MutableLiveData<>(Collections.emptyList());
        public final MutableLiveData<SyncState> sync = new MutableLiveData<>(SyncState.synced());
        public int refreshCalls;

        @Override
        public LiveData<List<Team>> observeTeams() {
            return teams;
        }

        @Override
        public LiveData<List<DailyChallenge>> observeChallenges() {
            return challenges;
        }

        @Override
        public LiveData<SyncState> syncState() {
            return sync;
        }

        @Override
        public void refresh(boolean force) {
            refreshCalls++;
        }

        @Override
        public Team getTeamSync(String id) {
            List<Team> list = teams.getValue();
            if (list == null) return null;
            for (Team t : list) if (t.getId().equals(id)) return t;
            return null;
        }
    }

    public static class FakeMatchRepository implements MatchRepository {
        public final List<MatchEntity> saved = new ArrayList<>();

        @Override
        public LiveData<List<MatchEntity>> observeHistory() {
            return new MutableLiveData<>(saved);
        }

        @Override
        public LiveData<MatchEntity> observeMatch(long id) {
            for (MatchEntity m : saved) if (m.id == id) return new MutableLiveData<>(m);
            return new MutableLiveData<>();
        }

        @Override
        public LiveData<List<MatchEntity>> observeBestScores(int limit) {
            return new MutableLiveData<>(saved);
        }

        @Override
        public LiveData<MatchStats> observeStats() {
            return new MutableLiveData<>(new MatchStats());
        }

        @Override
        public LiveData<Integer> observeChallengeWins(String challengeId) {
            return new MutableLiveData<>(0);
        }

        @Override
        public void save(MatchEntity match, SaveCallback callback) {
            match.id = saved.size() + 1;
            saved.add(match);
            callback.onSaved(match.id);
        }

        @Override
        public void delete(MatchEntity match) {
            saved.remove(match);
        }

        @Override
        public void restore(MatchEntity match) {
            saved.add(match);
        }

        @Override
        public void clearAll() {
            saved.clear();
        }
    }
}
