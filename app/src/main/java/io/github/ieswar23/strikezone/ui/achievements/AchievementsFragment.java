package io.github.ieswar23.strikezone.ui.achievements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.transition.MaterialSharedAxis;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.ieswar23.strikezone.databinding.FragmentAchievementsBinding;
import io.github.ieswar23.strikezone.util.InsetsUtil;

/** Badge cabinet: a header with the unlock count and a two-column grid of every achievement. */
@AndroidEntryPoint
public class AchievementsFragment extends Fragment {
    static final int SPAN_COUNT = 2;

    private FragmentAchievementsBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAchievementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.padTop(binding.appBar);
        InsetsUtil.padBottom(binding.badgeGrid);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        AchievementsHeaderAdapter header = new AchievementsHeaderAdapter();
        AchievementAdapter badges = new AchievementAdapter();
        ConcatAdapter adapter = new ConcatAdapter(header, badges);
        GridLayoutManager layout = new GridLayoutManager(requireContext(), SPAN_COUNT);
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position < header.getItemCount() ? SPAN_COUNT : 1;
            }
        });
        binding.badgeGrid.setLayoutManager(layout);
        binding.badgeGrid.setAdapter(adapter);

        AchievementsViewModel viewModel = new ViewModelProvider(this).get(AchievementsViewModel.class);
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.loading) return;
            header.submit(state);
            badges.submitList(state.badges);
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
