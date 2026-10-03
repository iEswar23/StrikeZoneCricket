package io.github.ieswar23.strikezone.data.repository;

import static com.google.common.truth.Truth.assertThat;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

import io.github.ieswar23.strikezone.data.local.MatchStats;
import io.github.ieswar23.strikezone.data.local.dao.AchievementDao;
import io.github.ieswar23.strikezone.data.local.dao.MatchDao;
import io.github.ieswar23.strikezone.data.local.entity.AchievementEntity;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.domain.achievements.Achievement;
import io.github.ieswar23.strikezone.domain.achievements.AchievementEvaluator;
import io.github.ieswar23.strikezone.domain.achievements.Badge;
import io.github.ieswar23.strikezone.domain.achievements.MatchRecord;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.util.AppExecutors;

public class DefaultAchievementRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantExecutor = new InstantTaskExecutorRule();

    private FakeMatchDao matchDao;
    private FakeAchievementDao achievementDao;
    private DefaultAchievementRepository repository;

    @Before
    public void setUp() {
        matchDao = new FakeMatchDao();
        achievementDao = new FakeAchievementDao();
        repository = new DefaultAchievementRepository(matchDao, achievementDao,
                new AppExecutors(Runnable::run, Runnable::run), new AchievementEvaluator(TimeZone.getTimeZone("UTC")));
    }

    private static MatchEntity match(long id, long playedAt, boolean won, int wickets) {
        MatchEntity m = new MatchEntity();
        m.id = id;
        m.playedAt = playedAt;
        m.overs = 2;
        m.won = won;
        m.wickets = wickets;
        m.runs = won ? 25 : 14;
        m.balls = 10;
        return m;
    }

    private List<Achievement> check(long matchId) {
        AtomicReference<List<Achievement>> result = new AtomicReference<>();
        repository.checkUnlocks(matchId, result::set);
        return result.get();
    }

    @Test
    public void newBadgesAreStoredAgainstTheMatchAndDatedByIt() {
        matchDao.rows.add(match(1, 1_000L, false, 2));
        matchDao.rows.add(match(2, 2_000L, true, 0));

        List<Achievement> unlocked = check(2);

        assertThat(unlocked).containsExactly(Achievement.FIRST_WIN, Achievement.FLAWLESS_CHASE).inOrder();
        assertThat(achievementDao.rows).hasSize(2);
        for (AchievementEntity row : achievementDao.rows) {
            assertThat(row.matchId).isEqualTo(2);
            assertThat(row.unlockedAt).isEqualTo(2_000L);
        }
    }

    @Test
    public void aBadgeIsOnlyUnlockedOnce() {
        matchDao.rows.add(match(1, 1_000L, true, 1));
        assertThat(check(1)).containsExactly(Achievement.FIRST_WIN);

        matchDao.rows.add(match(2, 2_000L, true, 1));
        assertThat(check(2)).isEmpty();
        assertThat(achievementDao.rows).hasSize(1);
        assertThat(achievementDao.rows.get(0).unlockedAt).isEqualTo(1_000L);
    }

    @Test
    public void backfillUnlocksOlderHistoryWithoutAMatch() {
        matchDao.rows.add(match(7, 1_000L, true, 1));
        long before = System.currentTimeMillis();

        repository.checkUnlocks(0, null);

        assertThat(achievementDao.rows).hasSize(1);
        assertThat(achievementDao.rows.get(0).id).isEqualTo(Achievement.FIRST_WIN.name());
        assertThat(achievementDao.rows.get(0).matchId).isEqualTo(0);
        assertThat(achievementDao.rows.get(0).unlockedAt).isAtLeast(before);
    }

    @Test
    public void badgesWaitForBothSourcesThenMergeStoredUnlocks() {
        List<List<Badge>> emitted = new ArrayList<>();
        repository.observeBadges().observeForever(emitted::add);
        assertThat(emitted).isEmpty();

        matchDao.all.setValue(new ArrayList<>());
        assertThat(emitted).containsExactly((List<Badge>) null);

        // Chase Master stays unlocked even though its matches were deleted from history.
        achievementDao.all.setValue(Arrays.asList(new AchievementEntity(Achievement.CHASE_MASTER.name(), 5_000L, 3),
                new AchievementEntity("RETIRED_BADGE", 1L, 1)));
        List<Badge> badges = emitted.get(emitted.size() - 1);
        assertThat(badges).hasSize(Achievement.values().length);
        assertThat(badges.get(0).getAchievement()).isEqualTo(Achievement.CHASE_MASTER);
        assertThat(badges.get(0).getUnlockedAt()).isEqualTo(5_000L);
        assertThat(AchievementEvaluator.unlockedCount(badges)).isEqualTo(1);

        matchDao.all.setValue(Arrays.asList(match(1, 1_000L, false, 1), match(2, 2_000L, false, 1)));
        badges = emitted.get(emitted.size() - 1);
        assertThat(badges.get(1).getAchievement()).isEqualTo(Achievement.FIRST_WIN);
        assertThat(badges.get(1).isUnlocked()).isFalse();
    }

    @Test
    public void unlockedByMatchMapsRowsToAchievementsInGridOrder() {
        List<List<Achievement>> emitted = new ArrayList<>();
        repository.observeUnlockedBy(4).observeForever(emitted::add);
        achievementDao.byMatch.setValue(Arrays.asList(
                new AchievementEntity(Achievement.PERFECT_TEN.name(), 10L, 4),
                new AchievementEntity("RETIRED_BADGE", 10L, 4),
                new AchievementEntity(Achievement.FIRST_WIN.name(), 10L, 4)));
        assertThat(achievementDao.observedMatchId).isEqualTo(4);
        assertThat(emitted.get(emitted.size() - 1))
                .containsExactly(Achievement.FIRST_WIN, Achievement.PERFECT_TEN).inOrder();
    }

    @Test
    public void clearAllDeletesEveryUnlock() {
        achievementDao.rows.add(new AchievementEntity(Achievement.FIRST_WIN.name(), 1L, 1));
        repository.clearAll();
        assertThat(achievementDao.rows).isEmpty();
    }

    @Test
    public void mapperCarriesEveryFactTheEvaluatorNeeds() {
        MatchEntity m = match(9, 123L, true, 0);
        m.mode = GameMode.DAILY_CHALLENGE.name();
        m.difficulty = "HARD";
        m.runs = 31;
        m.extras = 2;
        m.fours = 3;
        m.sixes = 2;
        m.perfectShots = 6;
        m.maxSixesInOver = 2;
        m.scoringOvers = 1;
        m.balls = 12;

        MatchRecord r = AchievementMapper.toRecord(m);

        assertThat(r.getId()).isEqualTo(9);
        assertThat(r.getPlayedAt()).isEqualTo(123L);
        assertThat(r.isWon()).isTrue();
        assertThat(r.isDailyChallenge()).isTrue();
        assertThat(r.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(r.getBatRuns()).isEqualTo(29);
        assertThat(r.getFours()).isEqualTo(3);
        assertThat(r.getSixes()).isEqualTo(2);
        assertThat(r.getPerfectShots()).isEqualTo(6);
        assertThat(r.getMaxSixesInOver()).isEqualTo(2);
        assertThat(r.getScoringOvers()).isEqualTo(1);
        assertThat(r.wonOffLastBall()).isTrue();
        assertThat(AchievementMapper.toRecords(null)).isEmpty();
    }

    // ------------------------------------------------------------------------------------------
    // In-memory DAOs
    // ------------------------------------------------------------------------------------------

    private static final class FakeMatchDao implements MatchDao {
        final List<MatchEntity> rows = new ArrayList<>();
        final MutableLiveData<List<MatchEntity>> all = new MutableLiveData<>();

        @Override
        public long insert(MatchEntity match) {
            rows.add(match);
            return match.id;
        }

        @Override
        public void delete(MatchEntity match) {
            rows.remove(match);
        }

        @Override
        public void deleteAll() {
            rows.clear();
        }

        @Override
        public LiveData<List<MatchEntity>> observeAll() {
            return all;
        }

        @Override
        public List<MatchEntity> getAllSync() {
            return new ArrayList<>(rows);
        }

        @Override
        public LiveData<MatchEntity> observeById(long id) {
            return new MutableLiveData<>();
        }

        @Override
        public LiveData<List<MatchEntity>> observeBestScores(int limit) {
            return new MutableLiveData<>();
        }

        @Override
        public LiveData<Integer> observeChallengeWins(String challengeId) {
            return new MutableLiveData<>(0);
        }

        @Override
        public LiveData<MatchStats> observeStats() {
            return new MutableLiveData<>(new MatchStats());
        }
    }

    private static final class FakeAchievementDao implements AchievementDao {
        final List<AchievementEntity> rows = new ArrayList<>();
        final MutableLiveData<List<AchievementEntity>> all = new MutableLiveData<>();
        final MutableLiveData<List<AchievementEntity>> byMatch = new MutableLiveData<>();
        long observedMatchId = -1;

        @Override
        public void insertAll(List<AchievementEntity> achievements) {
            for (AchievementEntity a : achievements) {
                boolean exists = false;
                for (AchievementEntity row : rows) exists |= row.id.equals(a.id);
                if (!exists) rows.add(a);
            }
        }

        @Override
        public LiveData<List<AchievementEntity>> observeAll() {
            return all;
        }

        @Override
        public LiveData<List<AchievementEntity>> observeUnlockedBy(long matchId) {
            observedMatchId = matchId;
            return byMatch;
        }

        @Override
        public List<AchievementEntity> getAllSync() {
            return new ArrayList<>(rows);
        }

        @Override
        public void deleteAll() {
            rows.clear();
        }
    }
}
