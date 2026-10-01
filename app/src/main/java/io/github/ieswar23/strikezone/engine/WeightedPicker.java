package io.github.ieswar23.strikezone.engine;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Picks an enum constant from integer weights using a supplied {@link Random}. Iteration order is
 * the enum's declaration order, so a seeded Random always produces the same pick.
 */
final class WeightedPicker<E extends Enum<E>> {
    private final EnumMap<E, Integer> weights;

    WeightedPicker(Class<E> type) {
        this.weights = new EnumMap<>(type);
    }

    WeightedPicker<E> set(E key, int weight) {
        weights.put(key, Math.max(0, weight));
        return this;
    }

    WeightedPicker<E> add(E key, int delta) {
        return set(key, get(key) + delta);
    }

    int get(E key) {
        Integer w = weights.get(key);
        return w == null ? 0 : w;
    }

    /** Moves all weight from one key to another. */
    WeightedPicker<E> transfer(E from, E to) {
        int w = get(from);
        set(from, 0);
        return add(to, w);
    }

    int total() {
        int sum = 0;
        for (int w : weights.values()) sum += w;
        return sum;
    }

    E pick(Random random) {
        int total = total();
        if (total <= 0) throw new IllegalStateException("No weights to pick from");
        int roll = random.nextInt(total);
        for (Map.Entry<E, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) return entry.getKey();
        }
        throw new IllegalStateException("Unreachable");
    }
}
