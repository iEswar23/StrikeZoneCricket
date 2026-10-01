package io.github.ieswar23.strikezone.engine;

import java.util.Random;

/** Random but fair Quick Chase targets: roughly 8–15 runs an over depending on difficulty. */
public final class TargetGenerator {

    private TargetGenerator() {
    }

    public static int quickChaseTarget(int overs, Difficulty difficulty, Random random) {
        int minPerOver;
        int maxPerOver;
        switch (difficulty) {
            case EASY:
                minPerOver = 8;
                maxPerOver = 11;
                break;
            case HARD:
                minPerOver = 12;
                maxPerOver = 15;
                break;
            default:
                minPerOver = 10;
                maxPerOver = 13;
                break;
        }
        int total = 0;
        for (int i = 0; i < overs; i++) {
            total += minPerOver + random.nextInt(maxPerOver - minPerOver + 1);
        }
        return Math.max(6, total + 1);
    }
}
