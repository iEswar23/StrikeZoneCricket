package io.github.ieswar23.strikezone.domain.achievements;

import io.github.ieswar23.strikezone.engine.Difficulty;

/**
 * The facts about one completed chase that achievements are judged on. A plain value object so
 * {@link AchievementEvaluator} has no Room or Android dependency; build one with {@link #builder()}.
 */
public final class MatchRecord {
    private final long id;
    private final long playedAt;
    private final boolean won;
    private final boolean dailyChallenge;
    private final Difficulty difficulty;
    private final int overs;
    private final int runs;
    private final int extras;
    private final int wickets;
    private final int balls;
    private final int fours;
    private final int sixes;
    private final int perfectShots;
    private final int maxSixesInOver;
    private final int scoringOvers;

    private MatchRecord(Builder b) {
        id = b.id;
        playedAt = b.playedAt;
        won = b.won;
        dailyChallenge = b.dailyChallenge;
        difficulty = b.difficulty == null ? Difficulty.MEDIUM : b.difficulty;
        overs = b.overs;
        runs = b.runs;
        extras = b.extras;
        wickets = b.wickets;
        balls = b.balls;
        fours = b.fours;
        sixes = b.sixes;
        perfectShots = b.perfectShots;
        maxSixesInOver = b.maxSixesInOver;
        scoringOvers = b.scoringOvers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getId() {
        return id;
    }

    public long getPlayedAt() {
        return playedAt;
    }

    public boolean isWon() {
        return won;
    }

    /** True if this was a Daily Challenge rather than a Quick Chase. */
    public boolean isDailyChallenge() {
        return dailyChallenge;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public int getOvers() {
        return overs;
    }

    public int getRuns() {
        return runs;
    }

    /** Runs scored off the bat (total minus extras). */
    public int getBatRuns() {
        return runs - extras;
    }

    public int getWickets() {
        return wickets;
    }

    /** Legal balls faced. */
    public int getBalls() {
        return balls;
    }

    public int getFours() {
        return fours;
    }

    public int getSixes() {
        return sixes;
    }

    public int getPerfectShots() {
        return perfectShots;
    }

    public int getMaxSixesInOver() {
        return maxSixesInOver;
    }

    /** Completed overs in which every legal ball was scored off. */
    public int getScoringOvers() {
        return scoringOvers;
    }

    /** True if the chase was won with the last legal ball of the innings. */
    public boolean wonOffLastBall() {
        return won && overs > 0 && balls >= overs * 6;
    }

    public static final class Builder {
        private long id;
        private long playedAt;
        private boolean won;
        private boolean dailyChallenge;
        private Difficulty difficulty = Difficulty.MEDIUM;
        private int overs = 2;
        private int runs;
        private int extras;
        private int wickets;
        private int balls;
        private int fours;
        private int sixes;
        private int perfectShots;
        private int maxSixesInOver;
        private int scoringOvers;

        private Builder() {
        }

        public Builder id(long value) {
            id = value;
            return this;
        }

        public Builder playedAt(long value) {
            playedAt = value;
            return this;
        }

        public Builder won(boolean value) {
            won = value;
            return this;
        }

        public Builder dailyChallenge(boolean value) {
            dailyChallenge = value;
            return this;
        }

        public Builder difficulty(Difficulty value) {
            difficulty = value;
            return this;
        }

        public Builder overs(int value) {
            overs = value;
            return this;
        }

        public Builder runs(int value) {
            runs = value;
            return this;
        }

        public Builder extras(int value) {
            extras = value;
            return this;
        }

        public Builder wickets(int value) {
            wickets = value;
            return this;
        }

        public Builder balls(int value) {
            balls = value;
            return this;
        }

        public Builder fours(int value) {
            fours = value;
            return this;
        }

        public Builder sixes(int value) {
            sixes = value;
            return this;
        }

        public Builder perfectShots(int value) {
            perfectShots = value;
            return this;
        }

        public Builder maxSixesInOver(int value) {
            maxSixesInOver = value;
            return this;
        }

        public Builder scoringOvers(int value) {
            scoringOvers = value;
            return this;
        }

        public MatchRecord build() {
            return new MatchRecord(this);
        }
    }
}
