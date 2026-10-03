package io.github.ieswar23.strikezone.ui.achievements;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.ieswar23.strikezone.data.repository.AchievementRepository;
import io.github.ieswar23.strikezone.util.LiveDataUtils;

@HiltViewModel
public class AchievementsViewModel extends ViewModel {
    private final LiveData<AchievementsUiState> state;

    @Inject
    public AchievementsViewModel(AchievementRepository repository) {
        state = LiveDataUtils.map(repository.observeBadges(), AchievementsUiState::from);
        // Unlock anything earned before achievements existed (matches saved by an older version).
        repository.checkUnlocks(0, null);
    }

    public LiveData<AchievementsUiState> getState() {
        return state;
    }
}
