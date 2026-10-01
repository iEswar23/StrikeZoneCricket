package io.github.ieswar23.strikezone.ui.howto;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.transition.MaterialSharedAxis;

import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.databinding.FragmentHowToPlayBinding;
import io.github.ieswar23.strikezone.engine.BowlerType;
import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.engine.MatchRules;
import io.github.ieswar23.strikezone.engine.TimingJudge;
import io.github.ieswar23.strikezone.util.InsetsUtil;

/** Rules and controls. Numbers are read from the engine so the copy never drifts from the code. */
public class HowToPlayFragment extends Fragment {
    private FragmentHowToPlayBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHowToPlayBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.padTop(binding.appBar);
        InsetsUtil.padBottom(binding.scrollContent);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        binding.timingWindows.setText(getString(R.string.howto_timing_windows,
                window(Difficulty.EASY), window(Difficulty.MEDIUM), window(Difficulty.HARD)));
        binding.wicketsRule.setText(getString(R.string.howto_wickets_rule,
                MatchRules.wicketsForOvers(1), MatchRules.wicketsForOvers(2), MatchRules.wicketsForOvers(5)));
        binding.bowlerTypes.setText(getString(R.string.howto_bowlers,
                BowlerType.PACE.getMinKph(), BowlerType.PACE.getMaxKph(),
                BowlerType.MEDIUM.getMinKph(), BowlerType.MEDIUM.getMaxKph(),
                BowlerType.SPIN.getMinKph(), BowlerType.SPIN.getMaxKph()));
    }

    private String window(Difficulty d) {
        return getString(R.string.howto_window_value, d.getLabel(),
                TimingJudge.perfectWindowMs(d, BowlerType.MEDIUM), TimingJudge.goodWindowMs(d, BowlerType.MEDIUM));
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
