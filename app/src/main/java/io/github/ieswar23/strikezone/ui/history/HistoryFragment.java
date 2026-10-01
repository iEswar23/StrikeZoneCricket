package io.github.ieswar23.strikezone.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialSharedAxis;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.databinding.FragmentHistoryBinding;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.ui.setup.MatchSetupFragment;
import io.github.ieswar23.strikezone.util.InsetsUtil;
import io.github.ieswar23.strikezone.util.NavUtils;

/** All completed chases, newest first, with filters and swipe-to-delete (undo via snackbar). */
@AndroidEntryPoint
public class HistoryFragment extends Fragment {
    private FragmentHistoryBinding binding;
    private HistoryViewModel viewModel;
    private MatchHistoryAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.padTop(binding.appBar);
        InsetsUtil.padBottom(binding.list);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        adapter = new MatchHistoryAdapter(this::openMatch);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                MatchEntity match = adapter.itemAt(pos);
                viewModel.delete(match);
                Snackbar.make(binding.getRoot(), R.string.history_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_undo, v -> viewModel.undoDelete(match))
                        .show();
            }
        }).attachToRecyclerView(binding.list);

        binding.filterGroup.setOnCheckedStateChangeListener((group, ids) -> {
            int id = ids.isEmpty() ? R.id.filterAll : ids.get(0);
            HistoryFilter f = id == R.id.filterWon ? HistoryFilter.WON
                    : id == R.id.filterLost ? HistoryFilter.LOST
                    : id == R.id.filterChallenges ? HistoryFilter.CHALLENGES : HistoryFilter.ALL;
            viewModel.setFilter(f);
        });
        binding.emptyCta.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString(MatchSetupFragment.ARG_MODE, GameMode.QUICK_CHASE.name());
            NavUtils.navigateSafely(NavHostFragment.findNavController(this), R.id.action_history_to_setup, args);
        });

        viewModel.getMatches().observe(getViewLifecycleOwner(), matches -> {
            adapter.submitList(matches);
            boolean empty = matches == null || matches.isEmpty();
            binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.list.setVisibility(empty ? View.GONE : View.VISIBLE);
            boolean filtered = viewModel.getFilter().getValue() != HistoryFilter.ALL;
            binding.emptyTitle.setText(filtered ? R.string.history_empty_filtered : R.string.history_empty_title);
            binding.emptyCta.setVisibility(filtered ? View.GONE : View.VISIBLE);
        });
    }

    private void openMatch(MatchEntity match) {
        Bundle args = new Bundle();
        args.putLong("matchId", match.id);
        NavUtils.navigateSafely(NavHostFragment.findNavController(this), R.id.action_history_to_summary, args);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
