package io.github.ieswar23.strikezone.ui.home;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.transition.MaterialSharedAxis;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.data.local.MatchStats;
import io.github.ieswar23.strikezone.databinding.FragmentHomeBinding;
import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.ui.setup.MatchSetupFragment;
import io.github.ieswar23.strikezone.util.Formatters;
import io.github.ieswar23.strikezone.util.InsetsUtil;
import io.github.ieswar23.strikezone.util.NavUtils;
import io.github.ieswar23.strikezone.util.UiState;

/** Landing screen with the animated title, daily challenge card and navigation tiles. */
@AndroidEntryPoint
public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private ObjectAnimator ballSpin;
    private ValueAnimator glowPulse;
    private ObjectAnimator shimmer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReenterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.padTop(binding.heroContent);
        InsetsUtil.padBottom(binding.scrollContent);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        NavController nav = NavHostFragment.findNavController(this);
        binding.playButton.setOnClickListener(v -> openSetup(nav, GameMode.QUICK_CHASE));
        binding.challengePlay.setOnClickListener(v -> openSetup(nav, GameMode.DAILY_CHALLENGE));
        binding.challengeCard.setOnClickListener(v -> openSetup(nav, GameMode.DAILY_CHALLENGE));
        binding.challengeRetry.setOnClickListener(v -> viewModel.retry());
        binding.tileHistory.setOnClickListener(v -> NavUtils.navigateSafely(nav, R.id.action_home_to_history, null));
        binding.tileStats.setOnClickListener(v -> NavUtils.navigateSafely(nav, R.id.action_home_to_stats, null));
        binding.tileHowTo.setOnClickListener(v -> NavUtils.navigateSafely(nav, R.id.action_home_to_howToPlay, null));
        binding.tileSettings.setOnClickListener(v -> NavUtils.navigateSafely(nav, R.id.action_home_to_settings, null));
        binding.tileAchievements.setOnClickListener(v -> NavUtils.navigateSafely(nav, R.id.action_home_to_achievements, null));
        binding.challengeDate.setText(Formatters.todayLabel());

        viewModel.getChallenge().observe(getViewLifecycleOwner(), this::renderChallenge);
        viewModel.getChallengeCompleted().observe(getViewLifecycleOwner(), done ->
                binding.challengeDone.setVisibility(Boolean.TRUE.equals(done) ? View.VISIBLE : View.GONE));
        viewModel.getStats().observe(getViewLifecycleOwner(), this::renderStats);
        viewModel.getAchievements().observe(getViewLifecycleOwner(), a -> {
            if (a == null || a.loading) return;
            binding.achievementsCount.setText(getString(R.string.home_achievements_count, a.unlocked, a.total));
        });

        if (savedInstanceState == null) animateTitle();
        startAmbientAnimations();
    }

    private void openSetup(NavController nav, GameMode mode) {
        Bundle args = new Bundle();
        args.putString(MatchSetupFragment.ARG_MODE, mode.name());
        NavUtils.navigateSafely(nav, R.id.action_home_to_setup, args);
    }

    private void renderChallenge(UiState<DailyChallenge> state) {
        // EMPTY means the sync finished but Room has not emitted the new rows yet: keep the skeleton.
        boolean loading = state.getStatus() == UiState.Status.LOADING || state.getStatus() == UiState.Status.EMPTY;
        boolean error = state.getStatus() == UiState.Status.ERROR;
        binding.challengeSkeleton.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.challengeError.setVisibility(error ? View.VISIBLE : View.GONE);
        binding.challengeBody.setVisibility(state.getStatus() == UiState.Status.CONTENT ? View.VISIBLE : View.GONE);
        if (loading) startShimmer(); else stopShimmer();
        DailyChallenge c = state.getData();
        if (c == null) return;
        binding.challengeBadge.setText(c.getBadge());
        binding.challengeTitle.setText(c.getTitle());
        binding.challengeDescription.setText(c.getDescription());
        binding.challengeTarget.setText(getString(R.string.home_challenge_target, c.getTarget(), c.getOvers(),
                c.getWickets(), c.getDifficulty().getLabel()));
    }

    private void renderStats(MatchStats stats) {
        int played = stats == null ? 0 : stats.totalMatches;
        binding.quickPlayed.setText(String.valueOf(played));
        binding.quickWins.setText(String.valueOf(stats == null ? 0 : stats.wins));
        binding.quickBest.setText(String.valueOf(stats == null ? 0 : stats.highestScore));
    }

    private void animateTitle() {
        View[] items = {binding.titleTop, binding.titleBottom, binding.tagline, binding.playButton};
        for (int i = 0; i < items.length; i++) {
            View v = items[i];
            v.setAlpha(0f);
            v.setTranslationY(40f);
            v.setScaleX(0.85f);
            v.setScaleY(0.85f);
            v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setStartDelay(120L * i + 150L).setDuration(520)
                    .setInterpolator(new OvershootInterpolator(1.6f)).start();
        }
    }

    private void startAmbientAnimations() {
        ballSpin = ObjectAnimator.ofFloat(binding.titleBall, View.ROTATION, 0f, 360f);
        ballSpin.setDuration(2400);
        ballSpin.setRepeatCount(ValueAnimator.INFINITE);
        ballSpin.setInterpolator(new LinearInterpolator());
        ballSpin.start();

        glowPulse = ValueAnimator.ofFloat(0f, 1f);
        glowPulse.setDuration(1800);
        glowPulse.setRepeatMode(ValueAnimator.REVERSE);
        glowPulse.setRepeatCount(ValueAnimator.INFINITE);
        glowPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        float density = getResources().getDisplayMetrics().density;
        int glow = requireContext().getColor(R.color.floodlight_yellow);
        glowPulse.addUpdateListener(a -> {
            if (binding == null) return;
            float f = (float) a.getAnimatedValue();
            binding.titleTop.setShadowLayer(6 * density + 14 * density * f, 0, 0, glow);
            binding.titleBall.setTranslationY(-6 * density * f);
        });
        glowPulse.start();
    }

    private void startShimmer() {
        if (shimmer != null) return;
        shimmer = ObjectAnimator.ofFloat(binding.challengeSkeleton, View.ALPHA, 1f, 0.45f);
        shimmer.setDuration(700);
        shimmer.setRepeatMode(ValueAnimator.REVERSE);
        shimmer.setRepeatCount(ValueAnimator.INFINITE);
        shimmer.start();
    }

    private void stopShimmer() {
        if (shimmer != null) {
            shimmer.cancel();
            shimmer = null;
        }
        if (binding != null) binding.challengeSkeleton.setAlpha(1f);
    }

    @Override
    public void onDestroyView() {
        if (ballSpin != null) ballSpin.cancel();
        if (glowPulse != null) glowPulse.cancel();
        stopShimmer();
        binding = null;
        super.onDestroyView();
    }
}
