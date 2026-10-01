package io.github.ieswar23.strikezone.engine;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.EnumSet;
import java.util.Random;

public class ShotResolverTest {

    private static Delivery delivery(Line line, Length length) {
        return new Delivery(BowlerType.MEDIUM, 124, 900, line, length, 0f);
    }

    @Test
    public void sameSeedAndInputsProduceIdenticalOutcomes() {
        ShotResolver a = new ShotResolver(new Random(42));
        ShotResolver b = new ShotResolver(new Random(42));
        DeliveryGenerator genA = new DeliveryGenerator(new Random(7));
        DeliveryGenerator genB = new DeliveryGenerator(new Random(7));
        FieldSetting field = FieldSetting.of(FieldSetting.Plan.BALANCED);
        ShotDirection[] dirs = ShotDirection.values();
        for (int i = 0; i < 200; i++) {
            ShotInput input = ShotInput.swing((i % 9 - 4) * 30L, dirs[i % dirs.length]);
            BallOutcome x = a.resolve(genA.next(BowlerType.PACE, Difficulty.MEDIUM), input, Difficulty.MEDIUM, field);
            BallOutcome y = b.resolve(genB.next(BowlerType.PACE, Difficulty.MEDIUM), input, Difficulty.MEDIUM, field);
            assertThat(x.getType()).isEqualTo(y.getType());
            assertThat(x.getDescription()).isEqualTo(y.getDescription());
            assertThat(x.getShotAngleDeg()).isEqualTo(y.getShotAngleDeg());
        }
    }

    @Test
    public void perfectShotWithTheLineIntoUnguardedZoneIsNeverOut() {
        FieldSetting attacking = FieldSetting.of(FieldSetting.Plan.ATTACKING);
        assertThat(attacking.hasDeepFielder(ShotDirection.STRAIGHT)).isFalse();
        Delivery ball = delivery(Line.ON_STUMPS, Length.GOOD);
        for (int seed = 0; seed < 500; seed++) {
            BallOutcome o = new ShotResolver(new Random(seed))
                    .resolve(ball, ShotInput.swing(0, ShotDirection.STRAIGHT), Difficulty.HARD, attacking);
            assertThat(o.isWicket()).isFalse();
            assertThat(o.getType()).isIn(EnumSet.of(OutcomeType.SIX, OutcomeType.FOUR, OutcomeType.TWO));
            assertThat(o.getTiming()).isEqualTo(Timing.PERFECT);
        }
    }

    @Test
    public void boundaryRidersCanCatchLoftedShots() {
        FieldSetting defensive = FieldSetting.of(FieldSetting.Plan.DEFENSIVE);
        Delivery ball = delivery(Line.ON_STUMPS, Length.GOOD);
        int caught = 0;
        for (int seed = 0; seed < 400; seed++) {
            BallOutcome o = new ShotResolver(new Random(seed))
                    .resolve(ball, ShotInput.swing(0, ShotDirection.STRAIGHT), Difficulty.MEDIUM, defensive);
            if (o.getType() == OutcomeType.CAUGHT) {
                caught++;
                assertThat(o.getDescription()).isEqualTo("Caught at long-off");
                assertThat(o.getFielderPosition()).isEqualTo("long-off");
            }
        }
        assertThat(caught).isGreaterThan(0);
    }

    @Test
    public void leavingAWideIsCalledWideAndIsNotALegalBall() {
        BallOutcome o = new ShotResolver(new Random(1))
                .resolve(delivery(Line.WIDE, Length.GOOD), ShotInput.leave(), Difficulty.MEDIUM,
                        FieldSetting.of(FieldSetting.Plan.BALANCED));
        assertThat(o.getType()).isEqualTo(OutcomeType.WIDE);
        assertThat(o.getType().isLegal()).isFalse();
        assertThat(o.getRuns()).isEqualTo(1);
        assertThat(o.getHeadline()).isEqualTo("WIDE");
    }

    @Test
    public void leavingBallOutsideOffIsAlwaysADot() {
        Delivery ball = delivery(Line.OUTSIDE_OFF, Length.GOOD);
        for (int seed = 0; seed < 100; seed++) {
            BallOutcome o = new ShotResolver(new Random(seed)).resolve(ball, ShotInput.leave(), Difficulty.HARD,
                    FieldSetting.of(FieldSetting.Plan.ATTACKING));
            assertThat(o.getType()).isEqualTo(OutcomeType.DOT);
            assertThat(o.getTiming()).isEqualTo(Timing.LEFT_ALONE);
        }
    }

    @Test
    public void missingAStraightFullBallUsuallyHitsTheStumps() {
        Delivery ball = delivery(Line.ON_STUMPS, Length.FULL);
        int bowled = 0;
        for (int seed = 0; seed < 300; seed++) {
            BallOutcome o = new ShotResolver(new Random(seed)).resolve(ball,
                    ShotInput.swing(-400, ShotDirection.STRAIGHT), Difficulty.MEDIUM, FieldSetting.of(FieldSetting.Plan.BALANCED));
            assertThat(o.getTiming()).isEqualTo(Timing.MISSED);
            assertThat(o.getRuns()).isEqualTo(0);
            if (o.getType() == OutcomeType.BOWLED) bowled++;
        }
        assertThat(bowled).isAtLeast(200);
        assertThat(bowled).isLessThan(300);
    }

    @Test
    public void perfectTimingScoresMoreThanEarlyTimingOnAverage() {
        FieldSetting field = FieldSetting.of(FieldSetting.Plan.BALANCED);
        Delivery ball = delivery(Line.OUTSIDE_OFF, Length.GOOD);
        long perfectRuns = 0;
        long earlyRuns = 0;
        for (int seed = 0; seed < 400; seed++) {
            perfectRuns += new ShotResolver(new Random(seed))
                    .resolve(ball, ShotInput.swing(0, ShotDirection.OFF), Difficulty.MEDIUM, field).getRuns();
            earlyRuns += new ShotResolver(new Random(seed))
                    .resolve(ball, ShotInput.swing(-100, ShotDirection.OFF), Difficulty.MEDIUM, field).getRuns();
        }
        assertThat(perfectRuns).isGreaterThan(earlyRuns * 2);
    }

    @Test
    public void shotPowerRewardsPlayingWithTheLine() {
        Delivery straight = delivery(Line.ON_STUMPS, Length.GOOD);
        assertThat(ShotResolver.shotPower(straight, ShotDirection.STRAIGHT, Timing.PERFECT)).isEqualTo(4);
        assertThat(ShotResolver.shotPower(straight, ShotDirection.OFF, Timing.PERFECT)).isEqualTo(3);
        assertThat(ShotResolver.shotPower(straight, ShotDirection.STRAIGHT, Timing.LATE)).isEqualTo(2);
        Delivery yorker = delivery(Line.ON_STUMPS, Length.YORKER);
        assertThat(ShotResolver.shotPower(yorker, ShotDirection.STRAIGHT, Timing.PERFECT)).isEqualTo(3);
        Delivery shortOff = delivery(Line.OUTSIDE_OFF, Length.SHORT);
        assertThat(ShotResolver.shotPower(shortOff, ShotDirection.LEG, Timing.PERFECT)).isEqualTo(4);
    }

    @Test
    public void timingJudgeClassifiesWindowBoundaries() {
        long perfect = TimingJudge.perfectWindowMs(Difficulty.MEDIUM, BowlerType.MEDIUM);
        long good = TimingJudge.goodWindowMs(Difficulty.MEDIUM, BowlerType.MEDIUM);
        assertThat(TimingJudge.classify(0, Difficulty.MEDIUM, BowlerType.MEDIUM)).isEqualTo(Timing.PERFECT);
        assertThat(TimingJudge.classify(-perfect, Difficulty.MEDIUM, BowlerType.MEDIUM)).isEqualTo(Timing.PERFECT);
        assertThat(TimingJudge.classify(-perfect - 1, Difficulty.MEDIUM, BowlerType.MEDIUM)).isEqualTo(Timing.EARLY);
        assertThat(TimingJudge.classify(good, Difficulty.MEDIUM, BowlerType.MEDIUM)).isEqualTo(Timing.LATE);
        assertThat(TimingJudge.classify(good + 1, Difficulty.MEDIUM, BowlerType.MEDIUM)).isEqualTo(Timing.MISSED);
    }

    @Test
    public void harderDifficultyShrinksTimingWindows() {
        for (BowlerType type : BowlerType.values()) {
            assertThat(TimingJudge.perfectWindowMs(Difficulty.HARD, type))
                    .isLessThan(TimingJudge.perfectWindowMs(Difficulty.MEDIUM, type));
            assertThat(TimingJudge.perfectWindowMs(Difficulty.MEDIUM, type))
                    .isLessThan(TimingJudge.perfectWindowMs(Difficulty.EASY, type));
            assertThat(TimingJudge.goodWindowMs(Difficulty.HARD, type))
                    .isLessThan(TimingJudge.goodWindowMs(Difficulty.EASY, type));
        }
        // Spin is more forgiving than pace at the same difficulty.
        assertThat(TimingJudge.perfectWindowMs(Difficulty.MEDIUM, BowlerType.SPIN))
                .isGreaterThan(TimingJudge.perfectWindowMs(Difficulty.MEDIUM, BowlerType.PACE));
    }

    @Test
    public void deliveryGeneratorRespectsBowlerSpeedBands() {
        DeliveryGenerator generator = new DeliveryGenerator(new Random(3));
        for (int i = 0; i < 300; i++) {
            for (BowlerType type : BowlerType.values()) {
                Delivery d = generator.next(type, Difficulty.MEDIUM);
                assertThat(d.getSpeedKph()).isAtLeast(type.getMinKph());
                assertThat(d.getSpeedKph()).isAtMost(type.getMaxKph());
                assertThat(Math.abs(d.getDeviation())).isAtMost(type.getMaxDeviation());
            }
        }
        // Faster balls arrive sooner; hard difficulty speeds everything up.
        assertThat(DeliveryGenerator.travelTimeMs(150, Difficulty.MEDIUM))
                .isLessThan(DeliveryGenerator.travelTimeMs(85, Difficulty.MEDIUM));
        assertThat(DeliveryGenerator.travelTimeMs(120, Difficulty.HARD))
                .isLessThan(DeliveryGenerator.travelTimeMs(120, Difficulty.EASY));
    }
}
