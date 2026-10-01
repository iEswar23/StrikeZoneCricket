package io.github.ieswar23.strikezone.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.ieswar23.strikezone.data.local.MatchStats;
import io.github.ieswar23.strikezone.data.repository.CricketRepository;
import io.github.ieswar23.strikezone.data.repository.MatchRepository;
import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.util.Formatters;
import io.github.ieswar23.strikezone.util.LiveDataUtils;
import io.github.ieswar23.strikezone.util.SyncState;
import io.github.ieswar23.strikezone.util.UiState;

@HiltViewModel
public class HomeViewModel extends ViewModel {
    private final CricketRepository cricketRepository;
    private final LiveData<UiState<DailyChallenge>> challenge;
    private final LiveData<Boolean> challengeCompleted;
    private final LiveData<MatchStats> stats;

    @Inject
    public HomeViewModel(CricketRepository cricketRepository, MatchRepository matchRepository) {
        this.cricketRepository = cricketRepository;
        LiveData<DailyChallenge> today = LiveDataUtils.map(cricketRepository.observeChallenges(),
                list -> DailyChallenge.forDay(list, Formatters.dayOfYear()));
        this.challenge = LiveDataUtils.combine(today, cricketRepository.syncState(), HomeViewModel::toUiState);
        this.challengeCompleted = LiveDataUtils.switchMap(today, c -> {
            if (c == null) return new MutableLiveData<>(false);
            return LiveDataUtils.map(matchRepository.observeChallengeWins(c.getId()), wins -> wins != null && wins > 0);
        });
        this.stats = matchRepository.observeStats();
    }

    static UiState<DailyChallenge> toUiState(DailyChallenge challenge, SyncState sync) {
        if (challenge != null) return UiState.content(challenge);
        if (sync == null || sync.getStatus() == SyncState.Status.SYNCING || sync.getStatus() == SyncState.Status.IDLE) {
            return UiState.loading(null);
        }
        if (sync.getStatus() == SyncState.Status.FAILED) return UiState.error(sync.getMessage(), null);
        return UiState.empty();
    }

    public LiveData<UiState<DailyChallenge>> getChallenge() {
        return challenge;
    }

    public LiveData<Boolean> getChallengeCompleted() {
        return challengeCompleted;
    }

    public LiveData<MatchStats> getStats() {
        return stats;
    }

    public void retry() {
        cricketRepository.refresh(true);
    }
}
