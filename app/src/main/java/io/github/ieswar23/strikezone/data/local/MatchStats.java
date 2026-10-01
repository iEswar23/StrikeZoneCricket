package io.github.ieswar23.strikezone.data.local;

/** Aggregate career numbers computed in SQL by {@code MatchDao#observeStats()}. */
public class MatchStats {
    public int totalMatches;
    public int wins;
    public int ties;
    public int highestScore;
    public int totalRuns;
    public int totalBalls;
    public int totalFours;
    public int totalSixes;
    public int challengesWon;
}
