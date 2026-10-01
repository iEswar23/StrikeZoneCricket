package io.github.ieswar23.strikezone.ui.stats;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.ieswar23.strikezone.data.local.MatchStats;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.data.repository.MatchRepository;

@HiltViewModel
public class StatsViewModel extends ViewModel {
    static final int BEST_SCORES = 5;

    private final LiveData<MatchStats> stats;
    private final LiveData<List<MatchEntity>> bestScores;

    @Inject
    public StatsViewModel(MatchRepository repository) {
        stats = repository.observeStats();
        bestScores = repository.observeBestScores(BEST_SCORES);
    }

    public LiveData<MatchStats> getStats() {
        return stats;
    }

    public LiveData<List<MatchEntity>> getBestScores() {
        return bestScores;
    }
}
