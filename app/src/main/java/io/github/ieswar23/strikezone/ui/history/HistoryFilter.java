package io.github.ieswar23.strikezone.ui.history;

import java.util.ArrayList;
import java.util.List;

import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;

public enum HistoryFilter {
    ALL, WON, LOST, CHALLENGES;

    public List<MatchEntity> apply(List<MatchEntity> matches) {
        List<MatchEntity> result = new ArrayList<>();
        if (matches == null) return result;
        for (MatchEntity m : matches) {
            boolean keep;
            switch (this) {
                case WON: keep = m.won; break;
                case LOST: keep = !m.won && !m.tied; break;
                case CHALLENGES: keep = m.challengeId != null; break;
                default: keep = true; break;
            }
            if (keep) result.add(m);
        }
        return result;
    }
}
