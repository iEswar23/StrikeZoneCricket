package io.github.ieswar23.strikezone.domain.achievements;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import io.github.ieswar23.strikezone.engine.Difficulty;

public class AchievementEvaluatorTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long HOUR = 60L * 60 * 1000;
    /** 2026-09-28 00:00 UTC. */
    private static final long MON = 20_724L * DAY;

    private final AchievementEvaluator evaluator = new AchievementEvaluator(TimeZone.getTimeZone("UTC"));

    /** A modest 2-over defeat that unlocks nothing on its own. */
    private static MatchRecord.Builder match() {
        return MatchRecord.builder().overs(2).runs(14).balls(12).wickets(1).fours(1).sixes(0).playedAt(MON);
    }

    private static MatchRecord.Builder win() {
        return match().won(true).runs(25).balls(10);
    }

    private AchievementProgress progress(Achievement a, MatchRecord... history) {
        for (AchievementProgress p : evaluator.evaluate(Arrays.asList(history))) {
            if (p.getAchievement() == a) return p;
        }
        throw new AssertionError("missing " + a);
    }

    private List<Achievement> unlocked(MatchRecord... history) {
        return evaluator.newlyUnlocked(Arrays.asList(history), Collections.emptySet());
    }

    // ------------------------------------------------------------------------------------------
    // Empty history and ordering
    // ------------------------------------------------------------------------------------------

    @Test
    public void emptyOrNullHistoryUnlocksNothingAndReportsZeroProgress() {
        List<AchievementProgress> all = evaluator.evaluate(Collections.emptyList());
        assertThat(all).hasSize(Achievement.values().length);
        for (int i = 0; i < all.size(); i++) {
            assertThat(all.get(i).getAchievement()).isEqualTo(Achievement.values()[i]);
            assertThat(all.get(i).getCurrent()).isEqualTo(0);
            assertThat(all.get(i).isComplete()).isFalse();
        }
        assertThat(evaluator.evaluate(null)).hasSize(Achievement.values().length);
        assertThat(evaluator.newlyUnlocked(null, null)).isEmpty();
    }

    @Test
    public void aPlainDefeatUnlocksNothing() {
        assertThat(unlocked(match().build())).isEmpty();
    }

    @Test
    public void thereAreTwelveBadgesWithPositiveTargets() {
        assertThat(Achievement.values()).hasLength(12);
        for (Achievement a : Achievement.values()) {
            assertThat(a.getTarget()).isAtLeast(1);
            assertThat(a.getTitle()).isNotEmpty();
            assertThat(a.getDescription()).isNotEmpty();
            assertThat(Achievement.fromId(a.name())).isEqualTo(a);
        }
        assertThat(Achievement.fromId("RETIRED_BADGE")).isNull();
        assertThat(Achievement.fromId(null)).isNull();
    }

    // ------------------------------------------------------------------------------------------
    // Every badge
    // ------------------------------------------------------------------------------------------

    @Test
    public void firstWinUnlocksOnAnyVictory() {
        assertThat(progress(Achievement.FIRST_WIN, match().build()).isComplete()).isFalse();
        assertThat(unlocked(win().build())).contains(Achievement.FIRST_WIN);
    }

    @Test
    public void firstFiftyCountsBatRunsOnlyAndTracksTheBestInnings() {
        AchievementProgress almost = progress(Achievement.FIRST_FIFTY,
                match().runs(30).build(), match().runs(52).extras(3).build());
        assertThat(almost.getCurrent()).isEqualTo(49);
        assertThat(almost.getProgressText()).isEqualTo("49/50");
        assertThat(almost.isComplete()).isFalse();

        AchievementProgress done = progress(Achievement.FIRST_FIFTY, match().runs(50).extras(0).build());
        assertThat(done.isComplete()).isTrue();
        // Two forties never add up to a fifty: it is a single-innings feat.
        assertThat(progress(Achievement.FIRST_FIFTY, match().runs(40).build(), match().runs(40).build()).isComplete())
                .isFalse();
    }

    @Test
    public void sixMachineNeedsThreeSixesInTheSameOver() {
        assertThat(progress(Achievement.SIX_MACHINE, match().sixes(5).maxSixesInOver(2).build()).getProgressText())
                .isEqualTo("2/3");
        assertThat(unlocked(match().sixes(3).maxSixesInOver(3).build())).contains(Achievement.SIX_MACHINE);
    }

    @Test
    public void perfectTenIsTenPerfectShotsInOneChase() {
        AchievementProgress p = progress(Achievement.PERFECT_TEN,
                match().perfectShots(7).build(), match().perfectShots(6).build());
        assertThat(p.getCurrent()).isEqualTo(7);
        assertThat(p.getPercent()).isEqualTo(70);
        // Nine is one short however many chases reach it; ten in one chase unlocks it.
        assertThat(unlocked(match().perfectShots(9).build(), match().perfectShots(9).build()))
                .doesNotContain(Achievement.PERFECT_TEN);
        assertThat(unlocked(match().perfectShots(10).build())).contains(Achievement.PERFECT_TEN);
    }

    @Test
    public void noDotBallOverUnlocksFromAnyScoringOver() {
        assertThat(progress(Achievement.DOT_FREE_OVER, match().scoringOvers(0).build()).isComplete()).isFalse();
        assertThat(unlocked(match().scoringOvers(1).build())).contains(Achievement.DOT_FREE_OVER);
    }

    @Test
    public void flawlessChaseNeedsAWinWithoutLosingAWicket() {
        assertThat(unlocked(win().wickets(0).build())).contains(Achievement.FLAWLESS_CHASE);
        assertThat(unlocked(win().wickets(1).build())).doesNotContain(Achievement.FLAWLESS_CHASE);
        // Losing without losing a wicket (ran out of balls) doesn't count.
        assertThat(unlocked(match().wickets(0).build())).doesNotContain(Achievement.FLAWLESS_CHASE);
    }

    @Test
    public void lastBallHeroNeedsTheWinningRunsOffTheFinalBall() {
        assertThat(unlocked(win().overs(2).balls(12).build())).contains(Achievement.LAST_BALL_HERO);
        assertThat(unlocked(win().overs(2).balls(11).build())).doesNotContain(Achievement.LAST_BALL_HERO);
        // Using every ball and still losing is not heroic.
        assertThat(unlocked(match().overs(2).balls(12).build())).doesNotContain(Achievement.LAST_BALL_HERO);
    }

    @Test
    public void hardModeWinnerNeedsAWinOnHard() {
        assertThat(unlocked(win().difficulty(Difficulty.HARD).build())).contains(Achievement.HARD_MODE_WINNER);
        assertThat(unlocked(win().difficulty(Difficulty.MEDIUM).build())).doesNotContain(Achievement.HARD_MODE_WINNER);
        assertThat(unlocked(match().difficulty(Difficulty.HARD).build())).doesNotContain(Achievement.HARD_MODE_WINNER);
    }

    @Test
    public void chaseMasterCountsCareerWins() {
        List<MatchRecord> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) history.add(win().build());
        history.add(match().build());
        AchievementProgress four = evaluator.evaluate(history).get(Achievement.CHASE_MASTER.ordinal());
        assertThat(four.getProgressText()).isEqualTo("4/5");

        history.add(win().build());
        assertThat(evaluator.newlyUnlocked(history, Collections.emptySet())).contains(Achievement.CHASE_MASTER);
    }

    @Test
    public void bigHitterAndBoundaryCenturyAreCareerTotals() {
        List<MatchRecord> history = new ArrayList<>();
        for (int i = 0; i < 9; i++) history.add(match().fours(5).sixes(5).build());
        AchievementProgress sixes = evaluator.evaluate(history).get(Achievement.BIG_HITTER.ordinal());
        AchievementProgress boundaries = evaluator.evaluate(history).get(Achievement.BOUNDARY_CENTURY.ordinal());
        assertThat(sixes.getProgressText()).isEqualTo("45/50");
        assertThat(boundaries.getProgressText()).isEqualTo("90/100");

        // One short of each target: 49 sixes and 99 boundaries.
        history.add(match().fours(5).sixes(4).build());
        assertThat(evaluator.newlyUnlocked(history, Collections.emptySet()))
                .containsNoneOf(Achievement.BIG_HITTER, Achievement.BOUNDARY_CENTURY);

        history.add(match().fours(0).sixes(1).build());
        assertThat(evaluator.newlyUnlocked(history, Collections.emptySet()))
                .containsAtLeast(Achievement.BIG_HITTER, Achievement.BOUNDARY_CENTURY);
    }

    // ------------------------------------------------------------------------------------------
    // Daily Challenge streak
    // ------------------------------------------------------------------------------------------

    private static MatchRecord daily(long playedAt, boolean won) {
        return MatchRecord.builder().dailyChallenge(true).won(won).playedAt(playedAt).overs(1).runs(19).balls(6).build();
    }

    @Test
    public void dailyChallengerNeedsWinsOnThreeConsecutiveDays() {
        AchievementProgress two = progress(Achievement.DAILY_CHALLENGER,
                daily(MON + 20 * HOUR, true), daily(MON + DAY + 9 * HOUR, true));
        assertThat(two.getProgressText()).isEqualTo("2/3");

        assertThat(unlocked(daily(MON + 20 * HOUR, true), daily(MON + DAY + 9 * HOUR, true),
                daily(MON + 2 * DAY + 23 * HOUR, true))).contains(Achievement.DAILY_CHALLENGER);
    }

    @Test
    public void dailyStreakIgnoresGapsLossesQuickChasesAndRepeatWinsOnOneDay() {
        MatchRecord quickWin = MatchRecord.builder().won(true).playedAt(MON + DAY).overs(1).balls(6).runs(19).build();
        AchievementProgress p = progress(Achievement.DAILY_CHALLENGER,
                daily(MON, true),
                daily(MON + 2 * HOUR, true),          // same day again: still one day
                daily(MON + DAY, false),              // lost the next day
                quickWin,                             // a Quick Chase win doesn't count
                daily(MON + 2 * DAY, true),
                daily(MON + 4 * DAY, true));          // gap on day 4
        assertThat(p.getCurrent()).isEqualTo(1);
    }

    @Test
    public void dailyStreakUsesTheLongestRunInAnyOrder() {
        AchievementProgress p = progress(Achievement.DAILY_CHALLENGER,
                daily(MON + 10 * DAY, true), daily(MON, true), daily(MON + 11 * DAY, true), daily(MON + DAY, true));
        assertThat(p.getCurrent()).isEqualTo(2);
    }

    @Test
    public void dailyStreakDaysFollowTheEvaluatorTimeZone() {
        // 23:30 and 00:30 UTC the next night are consecutive days in UTC but the same day in New York.
        MatchRecord late = daily(MON + 23 * HOUR + 30 * 60 * 1000, true);
        MatchRecord early = daily(MON + DAY + 30 * 60 * 1000, true);
        AchievementEvaluator utc = new AchievementEvaluator(TimeZone.getTimeZone("UTC"));
        AchievementEvaluator newYork = new AchievementEvaluator(TimeZone.getTimeZone("America/New_York"));
        assertThat(utc.longestDailyStreak(Arrays.asList(late, early))).isEqualTo(2);
        assertThat(newYork.longestDailyStreak(Arrays.asList(late, early))).isEqualTo(1);
    }

    // ------------------------------------------------------------------------------------------
    // Unlock bookkeeping and badge list
    // ------------------------------------------------------------------------------------------

    @Test
    public void alreadyUnlockedBadgesAreNotReportedAgain() {
        MatchRecord flawlessWin = win().wickets(0).build();
        assertThat(evaluator.newlyUnlocked(Collections.singletonList(flawlessWin), EnumSet.of(Achievement.FIRST_WIN)))
                .containsExactly(Achievement.FLAWLESS_CHASE);
    }

    @Test
    public void oneMatchCanUnlockSeveralBadgesInDeclarationOrder() {
        MatchRecord epic = win().difficulty(Difficulty.HARD).overs(5).balls(30).wickets(0).runs(66).extras(1)
                .sixes(4).maxSixesInOver(3).perfectShots(12).scoringOvers(1).build();
        assertThat(unlocked(epic)).containsExactly(Achievement.FIRST_WIN, Achievement.FIRST_FIFTY,
                Achievement.SIX_MACHINE, Achievement.PERFECT_TEN, Achievement.DOT_FREE_OVER,
                Achievement.FLAWLESS_CHASE, Achievement.LAST_BALL_HERO, Achievement.HARD_MODE_WINNER).inOrder();
    }

    @Test
    public void progressIsClampedToTheTarget() {
        AchievementProgress p = progress(Achievement.SIX_MACHINE, match().maxSixesInOver(6).build());
        assertThat(p.getCurrent()).isEqualTo(3);
        assertThat(p.getPercent()).isEqualTo(100);
        assertThat(new AchievementProgress(Achievement.CHASE_MASTER, -4).getCurrent()).isEqualTo(0);
    }

    @Test
    public void badgesPutUnlockedFirstAndKeepStoredUnlocksEvenIfHistoryIsGone() {
        Map<Achievement, Long> stored = new EnumMap<>(Achievement.class);
        stored.put(Achievement.CHASE_MASTER, 1_000L);
        stored.put(Achievement.FIRST_WIN, 500L);
        // History was cleared, so live progress is zero everywhere.
        List<Badge> badges = AchievementEvaluator.badges(evaluator.evaluate(Collections.emptyList()), stored);

        assertThat(badges).hasSize(12);
        assertThat(badges.get(0).getAchievement()).isEqualTo(Achievement.FIRST_WIN);
        assertThat(badges.get(1).getAchievement()).isEqualTo(Achievement.CHASE_MASTER);
        assertThat(badges.get(1).isUnlocked()).isTrue();
        assertThat(badges.get(1).getUnlockedAt()).isEqualTo(1_000L);
        assertThat(badges.get(1).getProgress().getProgressText()).isEqualTo("5/5");
        assertThat(badges.get(2).getAchievement()).isEqualTo(Achievement.FIRST_FIFTY);
        assertThat(badges.get(2).isUnlocked()).isFalse();
        assertThat(badges.get(2).getUnlockedAt()).isNull();
        assertThat(AchievementEvaluator.unlockedCount(badges)).isEqualTo(2);
        assertThat(AchievementEvaluator.unlockedCount(null)).isEqualTo(0);
    }

    @Test
    public void evaluationIsDeterministicAndOrderIndependent() {
        List<MatchRecord> history = new ArrayList<>(Arrays.asList(
                win().sixes(2).fours(3).perfectShots(4).build(),
                match().runs(51).sixes(3).maxSixesInOver(2).build(),
                daily(MON, true), daily(MON + DAY, true)));
        List<AchievementProgress> first = evaluator.evaluate(history);
        Collections.reverse(history);
        List<AchievementProgress> second = evaluator.evaluate(history);
        for (int i = 0; i < first.size(); i++) {
            assertThat(second.get(i).getCurrent()).isEqualTo(first.get(i).getCurrent());
        }
    }
}
