package io.github.ieswar23.strikezone.ui.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.data.repository.MatchRepository;
import io.github.ieswar23.strikezone.util.LiveDataUtils;

@HiltViewModel
public class HistoryViewModel extends ViewModel {
    private final MatchRepository repository;
    private final MutableLiveData<HistoryFilter> filter = new MutableLiveData<>(HistoryFilter.ALL);
    private final LiveData<List<MatchEntity>> matches;

    @Inject
    public HistoryViewModel(MatchRepository repository) {
        this.repository = repository;
        matches = LiveDataUtils.combine(repository.observeHistory(), filter, (list, f) ->
                (f == null ? HistoryFilter.ALL : f).apply(list));
    }

    public LiveData<List<MatchEntity>> getMatches() {
        return matches;
    }

    public LiveData<HistoryFilter> getFilter() {
        return filter;
    }

    public void setFilter(HistoryFilter value) {
        if (filter.getValue() != value) filter.setValue(value);
    }

    public void delete(MatchEntity match) {
        repository.delete(match);
    }

    public void undoDelete(MatchEntity match) {
        repository.restore(match);
    }
}
