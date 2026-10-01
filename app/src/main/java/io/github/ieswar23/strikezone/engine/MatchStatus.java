package io.github.ieswar23.strikezone.engine;

public enum MatchStatus {
    IN_PROGRESS,
    WON,
    LOST,
    TIED;

    public boolean isFinished() {
        return this != IN_PROGRESS;
    }
}
