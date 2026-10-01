package io.github.ieswar23.strikezone.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * State machine for a single chase. Records ball outcomes, counts legal deliveries into overs,
 * rotates bowlers and decides when the chase is won, lost or tied.
 */
public final class MatchEngine {
    private final MatchConfig config;
    private final List<Bowler> bowlers;
    private final List<Over> overs = new ArrayList<>();

    private int runs;
    private int wickets;
    private int legalBalls;
    private int fours;
    private int sixes;
    private int dots;
    private int extras;
    private MatchStatus status = MatchStatus.IN_PROGRESS;
    private boolean lastBallCompletedOver;

    public MatchEngine(MatchConfig config, List<Bowler> bowlers) {
        if (bowlers == null || bowlers.isEmpty()) throw new IllegalArgumentException("Need at least one bowler");
        this.config = config;
        this.bowlers = new ArrayList<>(bowlers);
        overs.add(new Over(0, this.bowlers.get(0)));
    }

    /** Records the outcome of the current delivery and returns the stored ball. */
    public Ball record(BallOutcome outcome) {
        return record(outcome.getType(), outcome.getDescription());
    }

    public Ball record(OutcomeType type, String description) {
        if (status.isFinished()) throw new IllegalStateException("Match is already finished: " + status);
        Over over = currentOver();
        Ball ball = over.add(type, description);

        runs += type.getRuns();
        if (type.isWicket()) wickets++;
        if (type.isLegal()) legalBalls++;
        if (type == OutcomeType.FOUR) fours++;
        if (type == OutcomeType.SIX) sixes++;
        if (type == OutcomeType.DOT) dots++;
        if (type == OutcomeType.WIDE) extras += type.getRuns();

        status = evaluate();
        lastBallCompletedOver = over.isComplete();
        if (!status.isFinished() && over.isComplete()) {
            int next = overs.size();
            overs.add(new Over(next, bowlers.get(next % bowlers.size())));
        }
        return ball;
    }

    private MatchStatus evaluate() {
        if (runs >= config.getTarget()) return MatchStatus.WON;
        boolean allOut = wickets >= config.getWicketsLimit();
        boolean oversDone = legalBalls >= config.getTotalBalls();
        if (allOut || oversDone) {
            return runs == config.getTarget() - 1 ? MatchStatus.TIED : MatchStatus.LOST;
        }
        return MatchStatus.IN_PROGRESS;
    }

    public MatchConfig getConfig() {
        return config;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public boolean isFinished() {
        return status.isFinished();
    }

    /** True if the most recently recorded ball was the sixth legal ball of its over. */
    public boolean lastBallCompletedOver() {
        return lastBallCompletedOver;
    }

    /** The over in progress (or the last over once the match has finished). */
    public Over currentOver() {
        return overs.get(overs.size() - 1);
    }

    public Bowler currentBowler() {
        return currentOver().getBowler();
    }

    public int currentOverIndex() {
        return currentOver().getIndex();
    }

    /** All overs that have at least one ball, in order. */
    public List<Over> getOvers() {
        List<Over> list = new ArrayList<>();
        for (Over o : overs) if (!o.isEmpty()) list.add(o);
        return Collections.unmodifiableList(list);
    }

    public int getRuns() {
        return runs;
    }

    public int getWickets() {
        return wickets;
    }

    public int getLegalBalls() {
        return legalBalls;
    }

    public int getFours() {
        return fours;
    }

    public int getSixes() {
        return sixes;
    }

    public int getDots() {
        return dots;
    }

    public int getExtras() {
        return extras;
    }

    /** Runs scored off the bat (total minus extras). */
    public int getBatRuns() {
        return runs - extras;
    }

    public int getRunsNeeded() {
        return Math.max(0, config.getTarget() - runs);
    }

    public int getBallsRemaining() {
        return Math.max(0, config.getTotalBalls() - legalBalls);
    }

    public int getWicketsRemaining() {
        return Math.max(0, config.getWicketsLimit() - wickets);
    }

    /** Runs per over, including the partial final over. */
    public int[] runsPerOver() {
        List<Over> played = getOvers();
        int[] result = new int[played.size()];
        for (int i = 0; i < played.size(); i++) result[i] = played.get(i).runs();
        return result;
    }

    /** Wickets per over, including the partial final over. */
    public int[] wicketsPerOver() {
        List<Over> played = getOvers();
        int[] result = new int[played.size()];
        for (int i = 0; i < played.size(); i++) result[i] = played.get(i).wickets();
        return result;
    }
}
