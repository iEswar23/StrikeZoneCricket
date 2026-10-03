package io.github.ieswar23.strikezone.data.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.ieswar23.strikezone.data.local.entity.AchievementEntity;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.domain.achievements.Achievement;
import io.github.ieswar23.strikezone.domain.achievements.MatchRecord;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.engine.Difficulty;

/** Converts Room rows into the plain inputs and outputs of the achievement evaluator. */
public final class AchievementMapper {

    private AchievementMapper() {
    }

    public static MatchRecord toRecord(MatchEntity m) {
        return MatchRecord.builder()
                .id(m.id)
                .playedAt(m.playedAt)
                .won(m.won)
                .dailyChallenge(GameMode.fromName(m.mode) == GameMode.DAILY_CHALLENGE)
                .difficulty(Difficulty.fromName(m.difficulty, Difficulty.MEDIUM))
                .overs(m.overs)
                .runs(m.runs)
                .extras(m.extras)
                .wickets(m.wickets)
                .balls(m.balls)
                .fours(m.fours)
                .sixes(m.sixes)
                .perfectShots(m.perfectShots)
                .maxSixesInOver(m.maxSixesInOver)
                .scoringOvers(m.scoringOvers)
                .build();
    }

    public static List<MatchRecord> toRecords(List<MatchEntity> matches) {
        if (matches == null) return Collections.emptyList();
        List<MatchRecord> records = new ArrayList<>(matches.size());
        for (MatchEntity m : matches) records.add(toRecord(m));
        return records;
    }

    /** Unlock time per achievement; rows for achievements that no longer exist are skipped. */
    public static Map<Achievement, Long> unlockTimes(List<AchievementEntity> rows) {
        Map<Achievement, Long> times = new EnumMap<>(Achievement.class);
        if (rows == null) return times;
        for (AchievementEntity row : rows) {
            Achievement a = Achievement.fromId(row.id);
            if (a != null) times.put(a, row.unlockedAt);
        }
        return times;
    }

    public static List<Achievement> toAchievements(List<AchievementEntity> rows) {
        List<Achievement> result = new ArrayList<>();
        if (rows == null) return result;
        for (AchievementEntity row : rows) {
            Achievement a = Achievement.fromId(row.id);
            if (a != null) result.add(a);
        }
        Collections.sort(result);
        return result;
    }
}
