package io.github.ieswar23.strikezone.ui.summary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.transition.MaterialFadeThrough;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.databinding.FragmentSummaryBinding;
import io.github.ieswar23.strikezone.databinding.ItemBadgeUnlockedBinding;
import io.github.ieswar23.strikezone.domain.achievements.Achievement;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.engine.Difficulty;
import io.github.ieswar23.strikezone.engine.ScoreCalculator;
import io.github.ieswar23.strikezone.ui.achievements.BadgeIcons;
import io.github.ieswar23.strikezone.ui.common.TeamBadge;
import io.github.ieswar23.strikezone.util.Formatters;
import io.github.ieswar23.strikezone.util.InsetsUtil;
import io.github.ieswar23.strikezone.util.NavUtils;

/** Post-match summary: result, any badges unlocked, batting card and Manhattan chart. */
@AndroidEntryPoint
public class SummaryFragment extends Fragment {
    private FragmentSummaryBinding binding;
    private SummaryViewModel viewModel;
    private boolean animatedIn;
    private boolean badgesAnimatedIn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.padTopAndBottom(binding.content);
        viewModel = new ViewModelProvider(this).get(SummaryViewModel.class);
        binding.homeButton.setOnClickListener(v -> goHome());
        binding.closeButton.setOnClickListener(v -> goHome());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goHome();
            }
        });
        viewModel.getMatch().observe(getViewLifecycleOwner(), match -> {
            if (match != null) render(match);
        });
        binding.badgeBanner.setOnClickListener(v -> NavUtils.navigateSafely(NavHostFragment.findNavController(this),
                R.id.action_summary_to_achievements, null));
        viewModel.getNewBadges().observe(getViewLifecycleOwner(), this::renderBadges);
    }

    private void renderBadges(List<Achievement> badges) {
        boolean any = badges != null && !badges.isEmpty();
        binding.badgeBanner.setVisibility(any ? View.VISIBLE : View.GONE);
        if (!any) return;
        binding.badgeBannerTitle.setText(getResources().getQuantityString(R.plurals.summary_badge_title,
                badges.size(), badges.size()));
        binding.badgeBannerList.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (Achievement a : badges) {
            ItemBadgeUnlockedBinding row = ItemBadgeUnlockedBinding.inflate(inflater, binding.badgeBannerList, false);
            BadgeIcons.bind(row.medal, a, true);
            row.title.setText(a.getTitle());
            row.description.setText(a.getDescription());
            binding.badgeBannerList.addView(row.getRoot());
        }
        if (!badgesAnimatedIn) {
            badgesAnimatedIn = true;
            View banner = binding.badgeBanner;
            banner.setAlpha(0f);
            banner.setScaleX(0.85f);
            banner.setScaleY(0.85f);
            banner.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(350).setDuration(450)
                    .setInterpolator(new OvershootInterpolator(1.8f)).start();
        }
    }

    private void goHome() {
        NavHostFragment.findNavController(this).popBackStack(R.id.homeFragment, false);
    }

    private void render(MatchEntity m) {
        int headline;
        int color;
        if (m.won) {
            headline = R.string.summary_victory;
            color = R.color.floodlight_yellow;
        } else if (m.tied) {
            headline = R.string.summary_tied;
            color = R.color.white;
        } else {
            headline = R.string.summary_defeat;
            color = R.color.wicket_red;
        }
        binding.resultHeadline.setText(headline);
        binding.resultHeadline.setTextColor(ContextCompat.getColor(requireContext(), color));
        binding.resultText.setText(m.resultText);
        binding.trophy.setText(m.won ? "🏆" : m.tied ? "🤝" : "🏏");
        TeamBadge.bind(binding.battingBadge, m.battingShort, m.battingColor);
        TeamBadge.bind(binding.bowlingBadge, m.bowlingShort, m.bowlingColor);
        binding.battingName.setText(m.battingTeamName);
        binding.bowlingName.setText(m.bowlingTeamName);
        binding.finalScore.setText(getString(R.string.summary_score, m.runs, m.wickets));
        binding.finalOvers.setText(getString(R.string.summary_overs, ScoreCalculator.formatOvers(m.balls), m.overs));
        binding.targetText.setText(getString(R.string.summary_target, m.target));
        binding.meta.setText(getString(R.string.summary_meta, GameMode.fromName(m.mode).getLabel(),
                Difficulty.fromName(m.difficulty, Difficulty.MEDIUM).getLabel(), Formatters.matchDate(m.playedAt)));

        int batRuns = m.runs - m.extras;
        binding.statRuns.setText(String.valueOf(batRuns));
        binding.statBalls.setText(String.valueOf(m.balls));
        binding.statFours.setText(String.valueOf(m.fours));
        binding.statSixes.setText(String.valueOf(m.sixes));
        binding.statStrikeRate.setText(ScoreCalculator.formatStrikeRate(batRuns, m.balls));
        binding.statDots.setText(String.valueOf(m.dots));
        binding.extrasText.setText(getString(R.string.summary_extras, m.extras,
                ScoreCalculator.formatRate(ScoreCalculator.runRate(m.runs, m.balls))));

        int[][] overs = SummaryViewModel.parseOvers(m.overSummary);
        binding.manhattan.setData(overs[0], overs[1], m.overs);

        binding.playAgainButton.setOnClickListener(v -> NavUtils.navigateSafely(NavHostFragment.findNavController(this),
                R.id.action_summary_to_game, SummaryViewModel.rematchOf(m).toBundle()));

        if (!animatedIn) {
            animatedIn = true;
            binding.resultHeadline.setScaleX(0.6f);
            binding.resultHeadline.setScaleY(0.6f);
            binding.resultHeadline.setAlpha(0f);
            binding.resultHeadline.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(500)
                    .setInterpolator(new OvershootInterpolator(2f)).start();
            binding.trophy.setRotation(-25f);
            binding.trophy.animate().rotation(0f).setDuration(700).setInterpolator(new OvershootInterpolator(3f)).start();
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
