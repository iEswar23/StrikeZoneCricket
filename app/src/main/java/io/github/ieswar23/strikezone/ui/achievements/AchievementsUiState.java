package io.github.ieswar23.strikezone.ui.achievements;

import java.util.Collections;
import java.util.List;

import io.github.ieswar23.strikezone.domain.achievements.AchievementEvaluator;
import io.github.ieswar23.strikezone.domain.achievements.Badge;

/** Everything the achievements screen renders: the grid plus the header summary. */
public final class AchievementsUiState {
    public final boolean loading;
    public final List<Badge> badges;
    public final int unlocked;
    public final int total;
    /** The locked badge closest to unlocking, or null when everything is unlocked. */
    public final Badge nextUp;

    private AchievementsUiState(boolean loading, List<Badge> badges, Badge nextUp) {
        this.loading = loading;
        this.badges = badges;
        this.unlocked = AchievementEvaluator.unlockedCount(badges);
        this.total = badges.size();
        this.nextUp = nextUp;
    }

    public static AchievementsUiState loading() {
        return new AchievementsUiState(true, Collections.emptyList(), null);
    }

    public static AchievementsUiState from(List<Badge> badges) {
        if (badges == null) return loading();
        return new AchievementsUiState(false, badges, closestLocked(badges));
    }

    /** Highest progress percentage wins; ties go to the earlier badge in the grid. */
    static Badge closestLocked(List<Badge> badges) {
        Badge best = null;
        for (Badge b : badges) {
            if (b.isUnlocked()) continue;
            if (best == null || b.getProgress().getPercent() > best.getProgress().getPercent()) best = b;
        }
        return best;
    }

    /** 0..100 share of badges unlocked, for the header ring. */
    public int unlockedPercent() {
        return total == 0 ? 0 : unlocked * 100 / total;
    }
}
