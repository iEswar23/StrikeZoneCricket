package io.github.ieswar23.strikezone.domain.model;

public enum GameMode {
    QUICK_CHASE("Quick Chase"),
    DAILY_CHALLENGE("Daily Challenge");

    private final String label;

    GameMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static GameMode fromName(String name) {
        if (name == null) return QUICK_CHASE;
        for (GameMode m : values()) {
            if (m.name().equalsIgnoreCase(name)) return m;
        }
        return QUICK_CHASE;
    }
}
