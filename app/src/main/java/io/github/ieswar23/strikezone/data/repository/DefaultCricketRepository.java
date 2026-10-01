package io.github.ieswar23.strikezone.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.github.ieswar23.strikezone.data.local.dao.ChallengeDao;
import io.github.ieswar23.strikezone.data.local.dao.TeamDao;
import io.github.ieswar23.strikezone.data.local.entity.ChallengeEntity;
import io.github.ieswar23.strikezone.data.local.entity.TeamEntity;
import io.github.ieswar23.strikezone.data.remote.CricketApi;
import io.github.ieswar23.strikezone.data.remote.dto.ChallengeDto;
import io.github.ieswar23.strikezone.data.remote.dto.ChallengesResponse;
import io.github.ieswar23.strikezone.data.remote.dto.TeamDto;
import io.github.ieswar23.strikezone.data.remote.dto.TeamsResponse;
import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.util.AppExecutors;
import io.github.ieswar23.strikezone.util.LiveDataUtils;
import io.github.ieswar23.strikezone.util.SyncState;
import retrofit2.Response;

@Singleton
public class DefaultCricketRepository implements CricketRepository {
    private final CricketApi api;
    private final TeamDao teamDao;
    private final ChallengeDao challengeDao;
    private final CatalogMapper mapper;
    private final AppExecutors executors;
    private final MutableLiveData<SyncState> syncState = new MutableLiveData<>(SyncState.idle());
    private final LiveData<List<Team>> teams;
    private final LiveData<List<DailyChallenge>> challenges;
    private boolean syncing;

    @Inject
    public DefaultCricketRepository(CricketApi api, TeamDao teamDao, ChallengeDao challengeDao,
                                    CatalogMapper mapper, AppExecutors executors) {
        this.api = api;
        this.teamDao = teamDao;
        this.challengeDao = challengeDao;
        this.mapper = mapper;
        this.executors = executors;
        this.teams = LiveDataUtils.map(teamDao.observeAll(), mapper::toTeams);
        this.challenges = LiveDataUtils.map(challengeDao.observeAll(), mapper::toChallenges);
    }

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
        return syncState;
    }

    @Override
    public void refresh(boolean force) {
        if (syncing) return;
        syncing = true;
        syncState.setValue(SyncState.syncing());
        executors.diskIO().execute(() -> {
            SyncState result;
            try {
                if (force || teamDao.count() == 0 || challengeDao.count() == 0) {
                    fetchTeams();
                    fetchChallenges();
                }
                result = SyncState.synced();
            } catch (IOException e) {
                result = SyncState.failed(e.getMessage() == null ? "Network error" : e.getMessage());
            }
            final SyncState finalResult = result;
            executors.mainThread().execute(() -> {
                syncing = false;
                syncState.setValue(finalResult);
            });
        });
    }

    private void fetchTeams() throws IOException {
        Response<TeamsResponse> response = api.getTeams().execute();
        TeamsResponse body = response.body();
        if (!response.isSuccessful() || body == null || body.teams == null) {
            throw new IOException("Could not load teams (HTTP " + response.code() + ")");
        }
        List<TeamEntity> entities = new ArrayList<>();
        for (TeamDto dto : body.teams) entities.add(mapper.toEntity(dto));
        teamDao.upsertAll(entities);
    }

    private void fetchChallenges() throws IOException {
        Response<ChallengesResponse> response = api.getDailyChallenges().execute();
        ChallengesResponse body = response.body();
        if (!response.isSuccessful() || body == null || body.challenges == null) {
            throw new IOException("Could not load daily challenges (HTTP " + response.code() + ")");
        }
        List<ChallengeEntity> entities = new ArrayList<>();
        for (ChallengeDto dto : body.challenges) entities.add(mapper.toEntity(dto));
        challengeDao.upsertAll(entities);
    }

    @Override
    public Team getTeamSync(String id) {
        TeamEntity entity = teamDao.getById(id);
        return entity == null ? null : mapper.toDomain(entity);
    }
}
