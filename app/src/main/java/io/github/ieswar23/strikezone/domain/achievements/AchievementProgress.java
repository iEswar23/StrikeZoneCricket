package io.github.ieswar23.strikezone.domain.achievements;

/** How far the player is towards one achievement, as computed from their match history. */
public final class AchievementProgress {
    private final Achievement achievement;
    private final int current;

    public AchievementProgress(Achievement achievement, int current) {
        this.achievement = achievement;
        this.current = Math.max(0, Math.min(current, achievement.getTarget()));
    }

    public Achievement getAchievement() {
        return achievement;
    }

    /** Tracked value, clamped to {@code 0..target}. */
    public int getCurrent() {
        return current;
    }

    public int getTarget() {
        return achievement.getTarget();
    }

    public boolean isComplete() {
        return current >= achievement.getTarget();
    }

    /** 0..100 for a progress bar. */
    public int getPercent() {
        return current * 100 / achievement.getTarget();
    }

    /** "7/10" style counter. */
    public String getProgressText() {
        return current + "/" + achievement.getTarget();
    }
}
