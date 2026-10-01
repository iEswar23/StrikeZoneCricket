package io.github.ieswar23.strikezone.engine;

import java.util.Random;

/**
 * Decides what happens to a delivery given the player's swing. Pure Java and fully deterministic
 * for a seeded {@link Random}, so every rule below is unit-tested.
 *
 * <p>The model:
 * <ol>
 *   <li>No swing: wides are called, straight balls may hit the stumps, everything else is a dot.</li>
 *   <li>Swing outside the timing window: a miss. Straight balls are likely bowled, balls outside
 *       off can take the edge.</li>
 *   <li>Contact: a "power" score from 0–4 starts at 4 (perfect) or 2 (early/late), loses a point
 *       for playing against the line and for yorkers. Power selects a weighted outcome table which
 *       is then adjusted for the field: boundary riders turn fours into twos and make lofted shots
 *       catchable; empty zones turn catches into runs.</li>
 * </ol>
 */
public final class ShotResolver {

    private enum Pick { DOT, ONE, TWO, THREE, FOUR, SIX, CAUGHT_DEEP, CAUGHT_INNER, RUN_OUT }

    static final String KEEPER = "wicket-keeper";

    private final Random random;

    public ShotResolver(Random random) {
        this.random = random;
    }

    public BallOutcome resolve(Delivery delivery, ShotInput input, Difficulty difficulty, FieldSetting field) {
        if (!input.isPlayed()) {
            return resolveLeave(delivery);
        }
        Timing timing = TimingJudge.classify(input.getOffsetMs(), difficulty, delivery.getBowlerType());
        if (timing == Timing.MISSED) {
            return resolveMiss(delivery, input, difficulty);
        }
        return resolveContact(delivery, input.getDirection(), timing, difficulty, field);
    }

    /** Power of a shot (0–4) before fielding adjustments. Exposed for tests. */
    static int shotPower(Delivery delivery, ShotDirection direction, Timing timing) {
        int power = timing == Timing.PERFECT ? 4 : 2;
        boolean natural = direction == delivery.getLine().getNaturalDirection();
        boolean pullingShortBall = delivery.getLength() == Length.SHORT && direction == ShotDirection.LEG;
        if (!natural && !pullingShortBall) power -= 1;
        if (delivery.getLength() == Length.YORKER) power -= 1;
        return Math.max(0, Math.min(4, power));
    }

    private BallOutcome resolveLeave(Delivery d) {
        if (d.isWide()) {
            return outcome(OutcomeType.WIDE, Timing.LEFT_ALONE, ShotDirection.STRAIGHT, 180, KEEPER,
                    "Wide ball, sensibly left alone");
        }
        if (d.isHittingStumps() && random.nextInt(100) < 70) {
            return outcome(OutcomeType.BOWLED, Timing.LEFT_ALONE, ShotDirection.STRAIGHT, 180, null,
                    "Bowled! Shouldered arms to a straight one");
        }
        String text = d.isHittingStumps() ? "Left it… whistles past off stump" : "Left alone outside the line";
        return outcome(OutcomeType.DOT, Timing.LEFT_ALONE, ShotDirection.STRAIGHT, 180, KEEPER, text);
    }

    private BallOutcome resolveMiss(Delivery d, ShotInput input, Difficulty difficulty) {
        boolean early = input.getOffsetMs() < 0;
        ShotDirection dir = input.getDirection();
        if (d.isWide()) {
            return outcome(OutcomeType.WIDE, Timing.MISSED, dir, 180, KEEPER, "Wide! Swung and missed at a wide one");
        }
        if (d.isHittingStumps()) {
            if (random.nextInt(100) < 80) {
                return outcome(OutcomeType.BOWLED, Timing.MISSED, dir, 180, null,
                        early ? "Bowled! Through the gate, too early" : "Bowled! Far too late on it");
            }
            return outcome(OutcomeType.DOT, Timing.MISSED, dir, 180, KEEPER, "Beaten! Inches over the bails");
        }
        int edgeChance = Math.round(18 * difficulty.getCatchFactor());
        if (d.getLine() == Line.OUTSIDE_OFF && random.nextInt(100) < edgeChance) {
            return outcome(OutcomeType.CAUGHT, Timing.MISSED, dir, 170, KEEPER, "Caught behind! Thin edge to the keeper");
        }
        return outcome(OutcomeType.DOT, Timing.MISSED, dir, 180, KEEPER,
                early ? "Swing and a miss, way too early" : "Swing and a miss, beaten for pace");
    }

    private BallOutcome resolveContact(Delivery d, ShotDirection dir, Timing timing,
                                       Difficulty difficulty, FieldSetting field) {
        int power = shotPower(d, dir, timing);
        WeightedPicker<Pick> table = baseTable(power);

        if (field.hasDeepFielder(dir)) {
            table.set(Pick.CAUGHT_DEEP, Math.round(table.get(Pick.CAUGHT_DEEP) * 1.6f * difficulty.getCatchFactor()));
            int savedFours = table.get(Pick.FOUR) / 2;
            table.add(Pick.FOUR, -savedFours).add(Pick.TWO, savedFours);
        } else {
            table.transfer(Pick.CAUGHT_DEEP, power >= 4 ? Pick.SIX : Pick.FOUR);
        }
        if (field.hasInnerFielder(dir)) {
            table.set(Pick.CAUGHT_INNER, Math.round(table.get(Pick.CAUGHT_INNER) * difficulty.getCatchFactor()));
        } else {
            table.transfer(Pick.CAUGHT_INNER, Pick.ONE);
            int gapRuns = table.get(Pick.DOT) / 2;
            table.add(Pick.DOT, -gapRuns).add(Pick.ONE, gapRuns);
        }

        Pick pick = table.pick(random);
        double spread = dir == ShotDirection.STRAIGHT ? 14 : 22;
        double angle = dir.getCentreAngleDeg() + (random.nextDouble() * 2 - 1) * spread;
        Fielder deep = field.deepFielder(dir);
        Fielder inner = field.innerFielder(dir);

        switch (pick) {
            case SIX:
                return outcome(OutcomeType.SIX, timing, dir, angle, null, sixText(dir));
            case FOUR:
                return outcome(OutcomeType.FOUR, timing, dir, angle, null, fourText(dir, timing));
            case THREE:
                return outcome(OutcomeType.THREE, timing, dir, angle, deep == null ? null : deep.getPosition(),
                        "Great running, they come back for three");
            case TWO:
                return outcome(OutcomeType.TWO, timing, dir, angle, deep == null ? null : deep.getPosition(),
                        deep == null ? "Into the gap " + zoneName(dir) + ", two runs"
                                : "Cut off by " + deep.getPosition() + ", they take two");
            case ONE:
                return outcome(OutcomeType.ONE, timing, dir, angle, inner == null ? null : inner.getPosition(),
                        inner == null ? "Worked " + zoneName(dir) + " for a single"
                                : "Pushed towards " + inner.getPosition() + " for a quick single");
            case CAUGHT_DEEP:
                return outcome(OutcomeType.CAUGHT, timing, dir, deep.getAngleDeg(), deep.getPosition(),
                        "Caught at " + deep.getPosition());
            case CAUGHT_INNER:
                return outcome(OutcomeType.CAUGHT, timing, dir, inner.getAngleDeg(), inner.getPosition(),
                        "Caught at " + inner.getPosition());
            case RUN_OUT: {
                Fielder thrower = inner != null ? inner : deep;
                String name = thrower == null ? "the bowler" : thrower.getPosition();
                double throwAngle = thrower == null ? 0 : thrower.getAngleDeg();
                return outcome(OutcomeType.RUN_OUT, timing, dir, throwAngle, thrower == null ? null : name,
                        "Run out going for the second! Direct hit from " + name);
            }
            case DOT:
            default:
                return outcome(OutcomeType.DOT, timing, dir, angle, inner == null ? null : inner.getPosition(),
                        inner == null ? "Defended, no run" : "Straight to " + inner.getPosition() + ", no run");
        }
    }

    private static WeightedPicker<Pick> baseTable(int power) {
        WeightedPicker<Pick> t = new WeightedPicker<>(Pick.class);
        switch (power) {
            case 4:
                return t.set(Pick.SIX, 46).set(Pick.FOUR, 40).set(Pick.TWO, 7).set(Pick.CAUGHT_DEEP, 7);
            case 3:
                return t.set(Pick.SIX, 18).set(Pick.FOUR, 44).set(Pick.THREE, 4).set(Pick.TWO, 16)
                        .set(Pick.ONE, 8).set(Pick.CAUGHT_DEEP, 10);
            case 2:
                return t.set(Pick.FOUR, 18).set(Pick.THREE, 6).set(Pick.TWO, 24).set(Pick.ONE, 34)
                        .set(Pick.DOT, 6).set(Pick.CAUGHT_INNER, 8).set(Pick.RUN_OUT, 4);
            case 1:
                return t.set(Pick.FOUR, 4).set(Pick.TWO, 10).set(Pick.ONE, 38).set(Pick.DOT, 30)
                        .set(Pick.CAUGHT_INNER, 13).set(Pick.RUN_OUT, 5);
            default:
                return t.set(Pick.ONE, 22).set(Pick.DOT, 56).set(Pick.CAUGHT_INNER, 17).set(Pick.RUN_OUT, 5);
        }
    }

    private String sixText(ShotDirection dir) {
        switch (dir) {
            case LEG: return "Launched over mid-wicket into the stands!";
            case OFF: return "Lofted inside-out over extra cover!";
            default: return "Straight back over the bowler's head!";
        }
    }

    private String fourText(ShotDirection dir, Timing timing) {
        String prefix = timing == Timing.PERFECT ? "Timed to perfection" : "Found the gap";
        switch (dir) {
            case LEG: return prefix + ", whipped through mid-wicket";
            case OFF: return prefix + ", driven through the covers";
            default: return prefix + ", punched down the ground";
        }
    }

    private static String zoneName(ShotDirection dir) {
        switch (dir) {
            case LEG: return "through mid-wicket";
            case OFF: return "through the off side";
            default: return "down the ground";
        }
    }

    private static BallOutcome outcome(OutcomeType type, Timing timing, ShotDirection dir, double angle,
                                       String fielder, String description) {
        return new BallOutcome(type, timing, dir, angle, fielder, headlineFor(type), description);
    }

    public static String headlineFor(OutcomeType type) {
        switch (type) {
            case SIX: return "SIX!";
            case FOUR: return "FOUR!";
            case THREE: return "3 RUNS";
            case TWO: return "2 RUNS";
            case ONE: return "1 RUN";
            case WIDE: return "WIDE";
            case CAUGHT:
            case BOWLED:
            case RUN_OUT: return "OUT!";
            case DOT:
            default: return "DOT BALL";
        }
    }
}
