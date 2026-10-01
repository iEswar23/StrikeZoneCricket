package io.github.ieswar23.strikezone.engine;

import java.util.Random;

/** Generates varied deliveries for a bowler type. Deterministic for a seeded {@link Random}. */
public final class DeliveryGenerator {
    private final Random random;

    public DeliveryGenerator(Random random) {
        this.random = random;
    }

    public Delivery next(BowlerType type, Difficulty difficulty) {
        int speed = type.getMinKph() + random.nextInt(type.getMaxKph() - type.getMinKph() + 1);

        Line line = new WeightedPicker<>(Line.class)
                .set(Line.WIDE, 4)
                .set(Line.OUTSIDE_OFF, 34)
                .set(Line.ON_STUMPS, 40)
                .set(Line.ON_PADS, 22)
                .pick(random);

        WeightedPicker<Length> lengths = new WeightedPicker<>(Length.class);
        switch (type) {
            case PACE:
                lengths.set(Length.YORKER, 12).set(Length.FULL, 28).set(Length.GOOD, 38).set(Length.SHORT, 22);
                break;
            case SPIN:
                lengths.set(Length.YORKER, 4).set(Length.FULL, 42).set(Length.GOOD, 46).set(Length.SHORT, 8);
                break;
            default:
                lengths.set(Length.YORKER, 8).set(Length.FULL, 34).set(Length.GOOD, 42).set(Length.SHORT, 16);
                break;
        }
        Length length = lengths.pick(random);

        float deviation = (random.nextFloat() * 2f - 1f) * type.getMaxDeviation();
        return new Delivery(type, speed, travelTimeMs(speed, difficulty), line, length, deviation);
    }

    /**
     * Arcade travel time: real deliveries take ~0.5s, which is too quick for a phone tap, so speed
     * is mapped onto a 0.7–1.2s window and then scaled by difficulty.
     */
    public static long travelTimeMs(int speedKph, Difficulty difficulty) {
        long base = Math.max(600L, 1650L - 6L * speedKph);
        return Math.round(base * difficulty.getTravelFactor());
    }
}
