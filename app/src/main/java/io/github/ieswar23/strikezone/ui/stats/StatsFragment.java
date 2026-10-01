package io.github.ieswar23.strikezone.ui.stats;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.transition.MaterialSharedAxis;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.data.local.MatchStats;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.databinding.FragmentStatsBinding;
import io.github.ieswar23.strikezone.databinding.ItemBestScoreBinding;
import io.github.ieswar23.strikezone.databinding.ViewStatTileBinding;
import io.github.ieswar23.strikezone.engine.ScoreCalculator;
import io.github.ieswar23.strikezone.ui.common.TeamBadge;
import io.github.ieswar23.strikezone.util.Formatters;
import io.github.ieswar23.strikezone.util.InsetsUtil;

/** Career numbers and personal-best chases. */
@AndroidEntryPoint
public class StatsFragment extends Fragment {
    private FragmentStatsBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        InsetsUtil.padTop(binding.appBar);
        InsetsUtil.padBottom(binding.scrollContent);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        StatsViewModel viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        label(binding.tileMatches, R.string.stats_matches, "🏏");
        label(binding.tileWins, R.string.stats_wins, "🏆");
        label(binding.tileHighest, R.string.stats_highest, "⭐");
        label(binding.tileSixes, R.string.stats_sixes, "🚀");
        label(binding.tileFours, R.string.stats_fours, "🎯");
        label(binding.tileChallenges, R.string.stats_challenges, "📅");

        viewModel.getStats().observe(getViewLifecycleOwner(), this::renderStats);
        viewModel.getBestScores().observe(getViewLifecycleOwner(), this::renderBest);
    }

    private void label(ViewStatTileBinding tile, int labelRes, String emoji) {
        tile.label.setText(labelRes);
        tile.emoji.setText(emoji);
    }

    private void renderStats(MatchStats s) {
        if (s == null) s = new MatchStats();
        boolean empty = s.totalMatches == 0;
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.statsContent.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) return;
        int winPct = ScoreCalculator.winPercentage(s.wins, s.totalMatches);
        countUp(binding.tileMatches.value, s.totalMatches, false);
        countUp(binding.tileWins.value, s.wins, false);
        countUp(binding.tileHighest.value, s.highestScore, false);
        countUp(binding.tileSixes.value, s.totalSixes, false);
        countUp(binding.tileFours.value, s.totalFours, false);
        countUp(binding.tileChallenges.value, s.challengesWon, false);
        countUp(binding.winPercent, winPct, true);
        binding.winRing.setProgressCompat(winPct, true);
        binding.winBreakdown.setText(getString(R.string.stats_breakdown, s.wins, s.totalMatches - s.wins - s.ties, s.ties));
        binding.careerLine.setText(getString(R.string.stats_career_line, s.totalRuns, s.totalBalls,
                ScoreCalculator.formatStrikeRate(s.totalRuns, s.totalBalls)));
    }

    private void renderBest(List<MatchEntity> best) {
        binding.bestList.removeAllViews();
        if (best == null) return;
        LayoutInflater inflater = getLayoutInflater();
        int rank = 1;
        for (MatchEntity m : best) {
            ItemBestScoreBinding row = ItemBestScoreBinding.inflate(inflater, binding.bestList, false);
            row.rank.setText(String.valueOf(rank++));
            TeamBadge.bind(row.badge, m.battingShort, m.battingColor);
            row.score.setText(getString(R.string.stats_best_score, m.runs, m.wickets, m.balls));
            row.detail.setText(getString(R.string.stats_best_detail, m.bowlingTeamName, Formatters.shortDate(m.playedAt)));
            row.result.setText(m.won ? R.string.history_won : m.tied ? R.string.history_tied : R.string.history_lost);
            row.result.setTextColor(requireContext().getColor(m.won ? R.color.grass_green : m.tied ? R.color.chip_dot : R.color.wicket_red));
            binding.bestList.addView(row.getRoot());
        }
    }

    private void countUp(TextView view, int value, boolean percent) {
        ValueAnimator animator = ValueAnimator.ofInt(0, value);
        animator.setDuration(Math.min(1200, 400 + value * 8L));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            int current = (int) a.getAnimatedValue();
            view.setText(percent ? view.getResources().getString(R.string.stats_percent, current) : String.valueOf(current));
        });
        animator.start();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
