package io.github.ieswar23.strikezone.domain.achievements;

/**
 * One tile on the achievements screen: the live progress plus, once earned, when it was unlocked.
 * A badge stays unlocked even if the matches that earned it are later deleted from history.
 */
public final class Badge {
    private final AchievementProgress progress;
    private final Long unlockedAt;

    public Badge(AchievementProgress progress, Long unlockedAt) {
        this.progress = progress;
        this.unlockedAt = unlockedAt;
    }

    public Achievement getAchievement() {
        return progress.getAchievement();
    }

    public AchievementProgress getProgress() {
        return progress;
    }

    public boolean isUnlocked() {
        return unlockedAt != null;
    }

    /** Epoch millis of the unlock, or null while locked. */
    public Long getUnlockedAt() {
        return unlockedAt;
    }
}
