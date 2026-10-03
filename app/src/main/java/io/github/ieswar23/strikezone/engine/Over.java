package io.github.ieswar23.strikezone.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Six legal deliveries from one bowler. Wides are recorded but do not count towards the six. */
public final class Over {
    private final int index;
    private final Bowler bowler;
    private final List<Ball> balls = new ArrayList<>();

    public Over(int index, Bowler bowler) {
        this.index = index;
        this.bowler = bowler;
    }

    public Ball add(OutcomeType type, String description) {
        return add(type, null, description);
    }

    public Ball add(OutcomeType type, Timing timing, String description) {
        if (isComplete()) throw new IllegalStateException("Over " + (index + 1) + " is already complete");
        Ball ball = new Ball(index, balls.size() + 1, type, timing, description);
        balls.add(ball);
        return ball;
    }

    public int getIndex() {
        return index;
    }

    public Bowler getBowler() {
        return bowler;
    }

    public List<Ball> getBalls() {
        return Collections.unmodifiableList(balls);
    }

    public int legalBalls() {
        int n = 0;
        for (Ball b : balls) if (b.isLegal()) n++;
        return n;
    }

    public int runs() {
        int n = 0;
        for (Ball b : balls) n += b.getRuns();
        return n;
    }

    public int wickets() {
        int n = 0;
        for (Ball b : balls) if (b.isWicket()) n++;
        return n;
    }

    /** How many balls of this over had the given outcome. */
    public int count(OutcomeType type) {
        int n = 0;
        for (Ball b : balls) if (b.getType() == type) n++;
        return n;
    }

    /**
     * True for a completed over in which the batter scored off every legal delivery: no dot balls
     * and no wickets. Wides are extras and are ignored.
     */
    public boolean isScoringOver() {
        if (!isComplete()) return false;
        for (Ball b : balls) {
            if (!b.isLegal()) continue;
            if (b.isWicket() || b.getType().getBatRuns() == 0) return false;
        }
        return true;
    }

    public boolean isComplete() {
        return legalBalls() >= MatchRules.BALLS_PER_OVER;
    }

    public boolean isEmpty() {
        return balls.isEmpty();
    }
}
