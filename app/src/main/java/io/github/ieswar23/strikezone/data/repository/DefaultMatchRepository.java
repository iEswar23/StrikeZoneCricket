package io.github.ieswar23.strikezone.data.repository;

import androidx.lifecycle.LiveData;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.github.ieswar23.strikezone.data.local.MatchStats;
import io.github.ieswar23.strikezone.data.local.dao.MatchDao;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.util.AppExecutors;

@Singleton
public class DefaultMatchRepository implements MatchRepository {
    private final MatchDao dao;
    private final AppExecutors executors;

    @Inject
    public DefaultMatchRepository(MatchDao dao, AppExecutors executors) {
        this.dao = dao;
        this.executors = executors;
    }

    @Override
    public LiveData<List<MatchEntity>> observeHistory() {
        return dao.observeAll();
    }

    @Override
    public LiveData<MatchEntity> observeMatch(long id) {
        return dao.observeById(id);
    }

    @Override
    public LiveData<List<MatchEntity>> observeBestScores(int limit) {
        return dao.observeBestScores(limit);
    }

    @Override
    public LiveData<MatchStats> observeStats() {
        return dao.observeStats();
    }

    @Override
    public LiveData<Integer> observeChallengeWins(String challengeId) {
        return dao.observeChallengeWins(challengeId);
    }

    @Override
    public void save(MatchEntity match, SaveCallback callback) {
        executors.diskIO().execute(() -> {
            long id = dao.insert(match);
            executors.mainThread().execute(() -> callback.onSaved(id));
        });
    }

    @Override
    public void delete(MatchEntity match) {
        executors.diskIO().execute(() -> dao.delete(match));
    }

    @Override
    public void restore(MatchEntity match) {
        executors.diskIO().execute(() -> dao.insert(match));
    }

    @Override
    public void clearAll() {
        executors.diskIO().execute(dao::deleteAll);
    }
}
