package io.github.ieswar23.strikezone.ui.summary;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.data.repository.MatchRepository;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.domain.model.MatchSetup;
import io.github.ieswar23.strikezone.engine.Difficulty;

@HiltViewModel
public class SummaryViewModel extends ViewModel {
    public static final String ARG_MATCH_ID = "matchId";

    private final LiveData<MatchEntity> match;

    @Inject
    public SummaryViewModel(SavedStateHandle handle, MatchRepository repository) {
        Long id = handle.get(ARG_MATCH_ID);
        match = repository.observeMatch(id == null ? -1L : id);
    }

    public LiveData<MatchEntity> getMatch() {
        return match;
    }

    /** Same teams, overs, target and difficulty: a straight rematch. */
    public static MatchSetup rematchOf(MatchEntity m) {
        return new MatchSetup(m.battingTeamId, m.bowlingTeamId, m.overs, m.target,
                Difficulty.fromName(m.difficulty, Difficulty.MEDIUM), GameMode.fromName(m.mode),
                m.challengeId, m.bowlerFocus);
    }

    /** Parses the "runs:wickets,..." over summary into two arrays. */
    public static int[][] parseOvers(String summary) {
        if (summary == null || summary.isEmpty()) return new int[][]{new int[0], new int[0]};
        String[] parts = summary.split(",");
        int[] runs = new int[parts.length];
        int[] wkts = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] rw = parts[i].split(":");
            try {
                runs[i] = Integer.parseInt(rw[0].trim());
                wkts[i] = rw.length > 1 ? Integer.parseInt(rw[1].trim()) : 0;
            } catch (NumberFormatException e) {
                runs[i] = 0;
                wkts[i] = 0;
            }
        }
        return new int[][]{runs, wkts};
    }
}
