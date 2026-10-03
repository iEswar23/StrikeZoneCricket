package io.github.ieswar23.strikezone.data.repository;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.github.ieswar23.strikezone.data.local.dao.AchievementDao;
import io.github.ieswar23.strikezone.data.local.dao.MatchDao;
import io.github.ieswar23.strikezone.data.local.entity.AchievementEntity;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.domain.achievements.Achievement;
import io.github.ieswar23.strikezone.domain.achievements.AchievementEvaluator;
import io.github.ieswar23.strikezone.domain.achievements.Badge;
import io.github.ieswar23.strikezone.util.AppExecutors;
import io.github.ieswar23.strikezone.util.LiveDataUtils;

@Singleton
public class DefaultAchievementRepository implements AchievementRepository {
    private final MatchDao matchDao;
    private final AchievementDao achievementDao;
    private final AppExecutors executors;
    private final AchievementEvaluator evaluator;

    @Inject
    public DefaultAchievementRepository(MatchDao matchDao, AchievementDao achievementDao, AppExecutors executors) {
        this(matchDao, achievementDao, executors, new AchievementEvaluator(TimeZone.getDefault()));
    }

    public DefaultAchievementRepository(MatchDao matchDao, AchievementDao achievementDao, AppExecutors executors,
                                        AchievementEvaluator evaluator) {
        this.matchDao = matchDao;
        this.achievementDao = achievementDao;
        this.executors = executors;
        this.evaluator = evaluator;
    }

    @Override
    public LiveData<List<Badge>> observeBadges() {
        return LiveDataUtils.combine(matchDao.observeAll(), achievementDao.observeAll(), (matches, unlocks) -> {
            if (matches == null || unlocks == null) return null;
            return AchievementEvaluator.badges(evaluator.evaluate(AchievementMapper.toRecords(matches)),
                    AchievementMapper.unlockTimes(unlocks));
        });
    }

    @Override
    public LiveData<List<Achievement>> observeUnlockedBy(long matchId) {
        return LiveDataUtils.map(achievementDao.observeUnlockedBy(matchId), AchievementMapper::toAchievements);
    }

    @Override
    public void checkUnlocks(long matchId, UnlockCallback callback) {
        executors.diskIO().execute(() -> {
            List<Achievement> unlocked = unlockNow(matchId);
            if (callback != null) executors.mainThread().execute(() -> callback.onChecked(unlocked));
        });
    }

    /**
     * Blocking: evaluates the history and inserts the new unlocks. Synchronized so two checks on the
     * disk pool can't both claim the same badge for different matches.
     */
    synchronized List<Achievement> unlockNow(long matchId) {
        List<MatchEntity> matches = matchDao.getAllSync();
        List<Achievement> already = AchievementMapper.toAchievements(achievementDao.getAllSync());
        List<Achievement> fresh = evaluator.newlyUnlocked(AchievementMapper.toRecords(matches), already);
        if (fresh.isEmpty()) return fresh;
        // Date the unlock by the match that earned it so history and badges agree.
        long unlockedAt = System.currentTimeMillis();
        if (matchId > 0) {
            for (MatchEntity m : matches) {
                if (m.id == matchId) unlockedAt = m.playedAt;
            }
        }
        List<AchievementEntity> rows = new ArrayList<>();
        for (Achievement a : fresh) rows.add(new AchievementEntity(a.name(), unlockedAt, Math.max(0, matchId)));
        achievementDao.insertAll(rows);
        return fresh;
    }

    @Override
    public void clearAll() {
        executors.diskIO().execute(achievementDao::deleteAll);
    }
}
