package io.github.ieswar23.strikezone.ui.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.MaterialSharedAxis;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.databinding.FragmentSetupBinding;
import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.domain.model.MatchSetup;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.util.InsetsUtil;
import io.github.ieswar23.strikezone.util.NavUtils;
import io.github.ieswar23.strikezone.util.UiState;

/** Pick teams, overs, difficulty and mode, then start the chase. */
@AndroidEntryPoint
public class MatchSetupFragment extends Fragment {
    public static final String ARG_MODE = "mode";

    private FragmentSetupBinding binding;
    private MatchSetupViewModel viewModel;
    private TeamAdapter teamAdapter;
    private TeamAdapter opponentAdapter;
    private boolean rendering;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.padTop(binding.appBar);
        InsetsUtil.padBottom(binding.bottomBar);
        viewModel = new ViewModelProvider(this).get(MatchSetupViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        teamAdapter = new TeamAdapter(team -> viewModel.selectTeam(team.getId()));
        opponentAdapter = new TeamAdapter(team -> viewModel.selectOpponent(team.getId()));
        binding.teamList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.teamList.setAdapter(teamAdapter);
        binding.opponentList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.opponentList.setAdapter(opponentAdapter);

        binding.modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || rendering) return;
            viewModel.setMode(checkedId == R.id.modeDaily ? GameMode.DAILY_CHALLENGE : GameMode.QUICK_CHASE);
        });
        binding.difficultyToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || rendering) return;
            Difficulty d = checkedId == R.id.difficultyEasy ? Difficulty.EASY
                    : checkedId == R.id.difficultyHard ? Difficulty.HARD : Difficulty.MEDIUM;
            viewModel.setDifficulty(d);
        });
        binding.oversGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (rendering || ids.isEmpty()) return;
            Chip chip = group.findViewById(ids.get(0));
            viewModel.setOvers(Integer.parseInt(String.valueOf(chip.getTag())));
        });
        binding.rerollButton.setOnClickListener(v -> viewModel.rerollTarget());
        binding.retryButton.setOnClickListener(v -> viewModel.retry());
        binding.startButton.setOnClickListener(v -> start());

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    private void start() {
        MatchSetup setup = viewModel.buildSetup();
        if (setup == null) {
            Snackbar.make(binding.getRoot(), R.string.setup_not_ready, Snackbar.LENGTH_SHORT).show();
            return;
        }
        NavUtils.navigateSafely(NavHostFragment.findNavController(this), R.id.action_setup_to_game, setup.toBundle());
    }

    private void render(SetupUiState s) {
        rendering = true;
        UiState.Status status = s.teams.getStatus();
        List<Team> teams = s.teams.getData();
        boolean hasTeams = teams != null && !teams.isEmpty();
        binding.loadingGroup.setVisibility(status == UiState.Status.LOADING && !hasTeams ? View.VISIBLE : View.GONE);
        binding.errorGroup.setVisibility(status == UiState.Status.ERROR && !hasTeams ? View.VISIBLE : View.GONE);
        binding.errorText.setText(s.teams.getMessage() == null ? getString(R.string.error_generic) : s.teams.getMessage());
        binding.teamList.setVisibility(hasTeams ? View.VISIBLE : View.INVISIBLE);
        binding.opponentList.setVisibility(hasTeams ? View.VISIBLE : View.INVISIBLE);
        binding.startButton.setEnabled(hasTeams);

        SetupSelection sel = s.selection;
        boolean daily = sel.mode == GameMode.DAILY_CHALLENGE;
        boolean dailyAvailable = daily && s.challenge != null;
        if (hasTeams) {
            teamAdapter.submitList(teams);
            opponentAdapter.submitList(teams);
            teamAdapter.setSelection(sel.teamId, sel.opponentId, false);
            opponentAdapter.setSelection(sel.opponentId, sel.teamId, dailyAvailable);
        }

        binding.modeToggle.check(daily ? R.id.modeDaily : R.id.modeQuick);
        binding.difficultyToggle.check(sel.difficulty == Difficulty.EASY ? R.id.difficultyEasy
                : sel.difficulty == Difficulty.HARD ? R.id.difficultyHard : R.id.difficultyMedium);
        binding.oversGroup.check(sel.overs == 1 ? R.id.overs1 : sel.overs == 5 ? R.id.overs5 : R.id.overs2);
        setEnabled(binding.difficultyToggle, !dailyAvailable);
        setEnabled(binding.oversGroup, !dailyAvailable);
        binding.rerollButton.setVisibility(daily ? View.GONE : View.VISIBLE);

        DailyChallenge c = s.challenge;
        binding.challengeCard.setVisibility(daily ? View.VISIBLE : View.GONE);
        if (daily && c != null) {
            binding.challengeTitle.setText(getString(R.string.setup_challenge_title, c.getBadge(), c.getTitle()));
            binding.challengeDescription.setText(c.getDescription());
        } else if (daily) {
            binding.challengeTitle.setText(R.string.setup_challenge_loading);
            binding.challengeDescription.setText("");
        }

        Team team = s.findTeam(sel.teamId);
        Team opponent = s.findTeam(sel.opponentId);
        binding.summaryMatchup.setText(team != null && opponent != null
                ? getString(R.string.setup_matchup, team.getShortName(), opponent.getShortName())
                : getString(R.string.setup_matchup_placeholder));
        binding.summaryTarget.setText(String.valueOf(s.target));
        binding.summaryDetails.setText(getString(R.string.setup_summary_details, sel.overs,
                sel.overs == 1 ? "" : "s", s.wickets, sel.difficulty.getLabel()));
        binding.summaryRate.setText(getString(R.string.setup_summary_rate,
                String.format(java.util.Locale.US, "%.1f", s.target * 1.0 / sel.overs)));
        rendering = false;
    }

    private static void setEnabled(ViewGroup group, boolean enabled) {
        group.setEnabled(enabled);
        group.setAlpha(enabled ? 1f : 0.5f);
        for (int i = 0; i < group.getChildCount(); i++) group.getChildAt(i).setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
