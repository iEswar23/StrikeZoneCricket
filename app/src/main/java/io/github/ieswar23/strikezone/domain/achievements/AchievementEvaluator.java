package io.github.ieswar23.strikezone.domain.achievements;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.TimeZone;

import io.github.ieswar23.strikezone.engine.Difficulty;

/**
 * Works out every {@link Achievement}'s progress from the player's match history. Pure Java and
 * deterministic: the same history (and time zone, which decides where a day starts for the Daily
 * Challenge streak) always gives the same result, so it is fully unit-tested.
 */
public final class AchievementEvaluator {
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private final TimeZone zone;

    public AchievementEvaluator(TimeZone zone) {
        this.zone = zone == null ? TimeZone.getTimeZone("UTC") : zone;
    }

    /** Progress for every achievement, in declaration order. */
    public List<AchievementProgress> evaluate(List<MatchRecord> history) {
        Map<Achievement, Integer> values = trackedValues(history);
        List<AchievementProgress> result = new ArrayList<>();
        for (Achievement a : Achievement.values()) result.add(new AchievementProgress(a, values.get(a)));
        return result;
    }

    /**
     * Achievements the history now satisfies that are not in {@code alreadyUnlocked}, in declaration
     * order. Call it after saving a match to find the badges that match just earned.
     */
    public List<Achievement> newlyUnlocked(List<MatchRecord> history, Collection<Achievement> alreadyUnlocked) {
        List<Achievement> result = new ArrayList<>();
        for (AchievementProgress p : evaluate(history)) {
            if (p.isComplete() && (alreadyUnlocked == null || !alreadyUnlocked.contains(p.getAchievement()))) {
                result.add(p.getAchievement());
            }
        }
        return result;
    }

    /**
     * Combines live progress with the stored unlock times. Unlocked badges come first, then locked
     * ones; both groups keep the declaration order so the grid doesn't reshuffle as numbers change.
     * A stored unlock wins over progress (badges are never taken away), and an unlocked badge shows
     * a full progress bar.
     */
    public static List<Badge> badges(List<AchievementProgress> progress, Map<Achievement, Long> unlockTimes) {
        List<Badge> unlocked = new ArrayList<>();
        List<Badge> locked = new ArrayList<>();
        for (AchievementProgress p : progress) {
            Long at = unlockTimes == null ? null : unlockTimes.get(p.getAchievement());
            if (at != null) {
                unlocked.add(new Badge(new AchievementProgress(p.getAchievement(), p.getTarget()), at));
            } else {
                locked.add(new Badge(p, null));
            }
        }
        unlocked.addAll(locked);
        return unlocked;
    }

    public static int unlockedCount(List<Badge> badges) {
        int n = 0;
        if (badges != null) for (Badge b : badges) if (b.isUnlocked()) n++;
        return n;
    }

    /** The number each achievement tracks: a best single-match effort or a career total. */
    private Map<Achievement, Integer> trackedValues(List<MatchRecord> history) {
        int wins = 0;
        int bestBatRuns = 0;
        int bestSixesInOver = 0;
        int bestPerfectShots = 0;
        int scoringOvers = 0;
        int flawlessWins = 0;
        int lastBallWins = 0;
        int hardWins = 0;
        int sixes = 0;
        int boundaries = 0;
        if (history != null) {
            for (MatchRecord m : history) {
                if (m == null) continue;
                bestBatRuns = Math.max(bestBatRuns, m.getBatRuns());
                bestSixesInOver = Math.max(bestSixesInOver, m.getMaxSixesInOver());
                bestPerfectShots = Math.max(bestPerfectShots, m.getPerfectShots());
                scoringOvers += m.getScoringOvers();
                sixes += m.getSixes();
                boundaries += m.getFours() + m.getSixes();
                if (m.isWon()) {
                    wins++;
                    if (m.getWickets() == 0) flawlessWins++;
                    if (m.wonOffLastBall()) lastBallWins++;
                    if (m.getDifficulty() == Difficulty.HARD) hardWins++;
                }
            }
        }
        Map<Achievement, Integer> values = new EnumMap<>(Achievement.class);
        values.put(Achievement.FIRST_WIN, wins);
        values.put(Achievement.FIRST_FIFTY, bestBatRuns);
        values.put(Achievement.SIX_MACHINE, bestSixesInOver);
        values.put(Achievement.PERFECT_TEN, bestPerfectShots);
        values.put(Achievement.DOT_FREE_OVER, scoringOvers);
        values.put(Achievement.FLAWLESS_CHASE, flawlessWins);
        values.put(Achievement.LAST_BALL_HERO, lastBallWins);
        values.put(Achievement.HARD_MODE_WINNER, hardWins);
        values.put(Achievement.CHASE_MASTER, wins);
        values.put(Achievement.DAILY_CHALLENGER, longestDailyStreak(history));
        values.put(Achievement.BIG_HITTER, sixes);
        values.put(Achievement.BOUNDARY_CENTURY, boundaries);
        return values;
    }

    /** Longest run of consecutive calendar days (in this evaluator's time zone) with a Daily Challenge win. */
    int longestDailyStreak(List<MatchRecord> history) {
        TreeSet<Long> days = new TreeSet<>();
        if (history != null) {
            for (MatchRecord m : history) {
                if (m != null && m.isWon() && m.isDailyChallenge()) days.add(localDay(m.getPlayedAt()));
            }
        }
        int best = 0;
        int run = 0;
        Long previous = null;
        for (long day : days) {
            run = previous != null && day == previous + 1 ? run + 1 : 1;
            best = Math.max(best, run);
            previous = day;
        }
        return best;
    }

    /** Days since the epoch, counted from local midnight. */
    long localDay(long epochMillis) {
        return Math.floorDiv(epochMillis + zone.getOffset(epochMillis), DAY_MS);
    }
}
