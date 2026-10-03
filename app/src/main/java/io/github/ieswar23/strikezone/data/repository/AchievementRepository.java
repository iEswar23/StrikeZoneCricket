package io.github.ieswar23.strikezone.data.repository;

import androidx.lifecycle.LiveData;

import java.util.List;

import io.github.ieswar23.strikezone.domain.achievements.Achievement;
import io.github.ieswar23.strikezone.domain.achievements.Badge;

/** Badge progress (derived from match history) and the persisted unlocks. */
public interface AchievementRepository {

    interface UnlockCallback {
        /** The achievements that were unlocked by this check, in declaration order (may be empty). */
        void onChecked(List<Achievement> newlyUnlocked);
    }

    /** Every badge with live progress and unlock time; null until both history and unlocks have loaded. */
    LiveData<List<Badge>> observeBadges();

    /** Badges that the given match unlocked, for the match summary banner. */
    LiveData<List<Achievement>> observeUnlockedBy(long matchId);

    /**
     * Evaluates the saved history in the background and stores any newly earned badges against
     * {@code matchId} (0 back-fills badges earned before achievements existed). The callback, if any,
     * runs on the main thread.
     */
    void checkUnlocks(long matchId, UnlockCallback callback);

    void clearAll();
}
