package io.github.ieswar23.strikezone.ui.game;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.databinding.FragmentGameBinding;
import io.github.ieswar23.strikezone.engine.BallOutcome;
import io.github.ieswar23.strikezone.engine.MatchRules;
import io.github.ieswar23.strikezone.engine.OutcomeType;
import io.github.ieswar23.strikezone.engine.ShotDirection;
import io.github.ieswar23.strikezone.engine.ShotInput;
import io.github.ieswar23.strikezone.engine.Timing;
import io.github.ieswar23.strikezone.ui.common.TeamBadge;
import io.github.ieswar23.strikezone.util.ColorUtils;
import io.github.ieswar23.strikezone.util.Haptics;
import io.github.ieswar23.strikezone.util.InsetsUtil;
import io.github.ieswar23.strikezone.util.NavUtils;

/** The batting screen: scoreboard HUD on top of the animated {@link PitchView}. */
@AndroidEntryPoint
public class GameFragment extends Fragment implements PitchView.Listener {
    private static final long SUMMARY_DELAY_MS = 1100L;

    @Inject
    Haptics haptics;

    private FragmentGameBinding binding;
    private GameViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int lastRenderedChips = -1;
    private boolean pauseDialogShowing;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGameBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.padTop(binding.hud);
        InsetsUtil.padBottom(binding.controls);
        viewModel = new ViewModelProvider(this).get(GameViewModel.class);

        binding.pitch.setListener(this);
        binding.pauseButton.setOnClickListener(v -> showPauseDialog());
        binding.errorBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showPauseDialog();
            }
        });

        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getLoadFailed().observe(getViewLifecycleOwner(), failed ->
                binding.errorState.setVisibility(Boolean.TRUE.equals(failed) ? View.VISIBLE : View.GONE));
        viewModel.getDeliveries().observe(getViewLifecycleOwner(), event -> {
            DeliveryCommand command = event.consume();
            if (command == null) return;
            binding.pitch.bowl(command);
            binding.bowlerLine.setText(getString(R.string.game_bowler_line, command.bowlerName,
                    command.delivery.getBowlerType().getLabel(), command.delivery.getSpeedKph()));
            highlightDirection(null);
        });
        viewModel.getOutcomes().observe(getViewLifecycleOwner(), event -> {
            BallOutcome outcome = event.consume();
            if (outcome == null) return;
            binding.pitch.playOutcome(outcome);
            showOutcome(outcome);
        });
        viewModel.getBanners().observe(getViewLifecycleOwner(), event -> {
            String text = event.consume();
            if (text != null) showBanner(text);
        });
        viewModel.getFinished().observe(getViewLifecycleOwner(), event -> {
            Long id = event.consume();
            if (id == null) return;
            handler.postDelayed(() -> openSummary(id), SUMMARY_DELAY_MS);
        });
        viewModel.onPitchReady();
    }

    private void openSummary(long matchId) {
        if (binding == null || !isAdded()) return;
        Bundle args = new Bundle();
        args.putLong("matchId", matchId);
        NavUtils.navigateSafely(NavHostFragment.findNavController(this), R.id.action_game_to_summary, args);
    }

    private void render(GameUiState s) {
        binding.loading.setVisibility(s.loading ? View.VISIBLE : View.GONE);
        if (s.loading) return;
        int batColor = ColorUtils.parse(s.battingColor, ContextCompat.getColor(requireContext(), R.color.brand_blue));
        int bowlColor = ColorUtils.parse(s.bowlingColor, ContextCompat.getColor(requireContext(), R.color.wicket_red));
        binding.pitch.setTeamColors(batColor, bowlColor);
        TeamBadge.bind(binding.teamBadge, s.battingShort, s.battingColor);
        binding.teamName.setText(getString(R.string.game_team_vs, s.battingName, s.bowlingShort));
        binding.score.setText(s.scoreText());
        binding.overs.setText(s.oversText);
        binding.oversLabel.setText(getString(R.string.game_overs_of, s.overs));
        binding.target.setText(getString(R.string.game_target, s.target));
        binding.equation.setText(s.equationText);
        binding.crr.setText(s.currentRunRate);
        binding.rrr.setText(s.requiredRunRate);
        binding.wickets.setText(getString(R.string.game_wickets_value, s.wickets, s.wicketsLimit));
        binding.modeChip.setText(s.modeLabel);
        binding.fieldChip.setText(getString(R.string.game_field, s.fieldPlan));
        int progress = (int) Math.min(100, Math.round(s.runs * 100.0 / Math.max(1, s.target)));
        binding.chaseProgress.setProgressCompat(progress, true);
        renderChips(s);
    }

    private void renderChips(GameUiState s) {
        int signature = s.thisOver.hashCode() * 31 + s.legalBalls;
        if (signature == lastRenderedChips) return;
        lastRenderedChips = signature;
        LinearLayout row = binding.thisOverChips;
        row.removeAllViews();
        int size = getResources().getDimensionPixelSize(R.dimen.ball_chip_size);
        int gap = getResources().getDimensionPixelSize(R.dimen.space_xs);
        int legal = 0;
        for (OutcomeType type : s.thisOver) {
            row.addView(makeChip(type.getChip(), chipColor(type), type == OutcomeType.SIX, size, gap));
            if (type.isLegal()) legal++;
        }
        for (int i = legal; i < MatchRules.BALLS_PER_OVER; i++) {
            TextView empty = makeChip("", 0, false, size, gap);
            empty.setBackgroundResource(R.drawable.bg_ball_chip_empty);
            empty.setBackgroundTintList(null);
            row.addView(empty);
        }
    }

    private TextView makeChip(String text, int color, boolean darkText, int size, int gap) {
        TextView chip = new TextView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMarginEnd(gap);
        chip.setLayoutParams(lp);
        chip.setGravity(Gravity.CENTER);
        chip.setText(text);
        chip.setTextAppearance(R.style.TextAppearance_StrikeZone_Chip);
        chip.setTextColor(ContextCompat.getColor(requireContext(), darkText ? R.color.night_900 : R.color.white));
        chip.setBackgroundResource(R.drawable.bg_ball_chip);
        if (color != 0) chip.setBackgroundTintList(ColorStateList.valueOf(color));
        return chip;
    }

    private int chipColor(OutcomeType type) {
        int res;
        switch (type) {
            case DOT: res = R.color.chip_dot; break;
            case FOUR: res = R.color.grass_green; break;
            case SIX: res = R.color.floodlight_yellow; break;
            case WIDE: res = R.color.chip_wide; break;
            case CAUGHT:
            case BOWLED:
            case RUN_OUT: res = R.color.wicket_red; break;
            default: res = R.color.chip_run; break;
        }
        return ContextCompat.getColor(requireContext(), res);
    }

    private void showOutcome(BallOutcome outcome) {
        OutcomeType type = outcome.getType();
        int color;
        if (type == OutcomeType.SIX) {
            color = R.color.floodlight_yellow;
            haptics.boundary(binding.getRoot());
        } else if (type == OutcomeType.FOUR) {
            color = R.color.grass_green;
            haptics.boundary(binding.getRoot());
        } else if (type.isWicket()) {
            color = R.color.wicket_red;
            haptics.wicket(binding.getRoot());
        } else {
            color = R.color.white;
        }
        TextView headline = binding.outcomeHeadline;
        headline.animate().cancel();
        binding.outcomeDetail.animate().cancel();
        headline.setText(outcome.getHeadline());
        headline.setTextColor(ContextCompat.getColor(requireContext(), color));
        binding.outcomeDetail.setText(outcome.getDescription());
        binding.timingLabel.setText(timingText(outcome.getTiming()));
        binding.timingLabel.setTextColor(ContextCompat.getColor(requireContext(),
                outcome.getTiming() == Timing.PERFECT ? R.color.grass_green : R.color.floodlight_yellow));
        binding.timingLabel.setVisibility(outcome.getTiming().madeContact() ? View.VISIBLE : View.INVISIBLE);

        View popup = binding.outcomePopup;
        popup.animate().cancel();
        popup.setVisibility(View.VISIBLE);
        popup.setAlpha(0f);
        popup.setTranslationY(0f);
        popup.setScaleX(0.4f);
        popup.setScaleY(0.4f);
        long hold = type.isWicket() || type.isBoundary() ? 1100 : 750;
        popup.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(320)
                .setInterpolator(new OvershootInterpolator(2.2f))
                .withEndAction(() -> popup.animate().alpha(0f).translationY(-popup.getHeight() * 0.3f)
                        .setStartDelay(hold).setDuration(260).setInterpolator(new DecelerateInterpolator()))
                .start();
    }

    private String timingText(Timing timing) {
        switch (timing) {
            case PERFECT: return getString(R.string.timing_perfect);
            case EARLY: return getString(R.string.timing_early);
            case LATE: return getString(R.string.timing_late);
            default: return "";
        }
    }

    private void showBanner(String text) {
        TextView banner = binding.banner;
        banner.animate().cancel();
        banner.setText(text);
        banner.setVisibility(View.VISIBLE);
        banner.setAlpha(0f);
        banner.setTranslationY(-24f);
        banner.animate().alpha(1f).translationY(0f).setStartDelay(0).setDuration(260)
                .withEndAction(() -> banner.animate().alpha(0f).setStartDelay(1500).setDuration(300)
                        .withEndAction(() -> banner.setVisibility(View.GONE)))
                .start();
    }

    private void highlightDirection(@Nullable ShotDirection direction) {
        setDirectionState(binding.dirLeg, direction == ShotDirection.LEG);
        setDirectionState(binding.dirStraight, direction == ShotDirection.STRAIGHT);
        setDirectionState(binding.dirOff, direction == ShotDirection.OFF);
    }

    private void setDirectionState(TextView view, boolean active) {
        view.setSelected(active);
        view.setAlpha(active ? 1f : 0.7f);
        view.animate().scaleX(active ? 1.08f : 1f).scaleY(active ? 1.08f : 1f).setDuration(120).start();
    }

    private void showPauseDialog() {
        if (pauseDialogShowing) return;
        pauseDialogShowing = true;
        binding.pitch.pause();
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pause_title)
                .setMessage(R.string.pause_message)
                .setPositiveButton(R.string.pause_resume, null)
                .setNegativeButton(R.string.pause_quit, (d, w) ->
                        NavHostFragment.findNavController(this).popBackStack(R.id.homeFragment, false))
                .setOnDismissListener(d -> {
                    pauseDialogShowing = false;
                    if (binding != null) binding.pitch.resume();
                })
                .show();
    }

    // PitchView.Listener ---------------------------------------------------------------------

    @Override
    public void onShot(ShotInput input) {
        viewModel.playShot(input);
    }

    @Override
    public void onSwing(ShotDirection direction) {
        haptics.tick(binding.pitch);
        highlightDirection(direction);
    }

    @Override
    public void onBallFinished() {
        viewModel.onBallAnimationDone();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) binding.pitch.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null && !pauseDialogShowing) binding.pitch.resume();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        binding.pitch.setListener(null);
        binding = null;
        super.onDestroyView();
    }
}
