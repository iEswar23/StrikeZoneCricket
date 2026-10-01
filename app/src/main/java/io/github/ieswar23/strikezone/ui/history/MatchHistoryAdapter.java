package io.github.ieswar23.strikezone.ui.history;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.databinding.ItemMatchBinding;
import io.github.ieswar23.strikezone.domain.model.GameMode;
import io.github.ieswar23.strikezone.engine.ScoreCalculator;
import io.github.ieswar23.strikezone.ui.common.TeamBadge;
import io.github.ieswar23.strikezone.util.Formatters;

public class MatchHistoryAdapter extends ListAdapter<MatchEntity, MatchHistoryAdapter.MatchViewHolder> {

    public interface OnMatchClick {
        void onMatchClick(MatchEntity match);
    }

    private static final DiffUtil.ItemCallback<MatchEntity> DIFF = new DiffUtil.ItemCallback<MatchEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull MatchEntity a, @NonNull MatchEntity b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull MatchEntity a, @NonNull MatchEntity b) {
            return a.runs == b.runs && a.wickets == b.wickets && a.balls == b.balls && a.won == b.won
                    && a.playedAt == b.playedAt && a.resultText.equals(b.resultText);
        }
    };

    private final OnMatchClick listener;

    public MatchHistoryAdapter(OnMatchClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    public MatchEntity itemAt(int position) {
        return getItem(position);
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MatchViewHolder(ItemMatchBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class MatchViewHolder extends RecyclerView.ViewHolder {
        private final ItemMatchBinding binding;

        MatchViewHolder(ItemMatchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MatchEntity m) {
            Context ctx = binding.getRoot().getContext();
            TeamBadge.bind(binding.badge, m.battingShort, m.battingColor);
            binding.matchup.setText(ctx.getString(R.string.history_matchup, m.battingTeamName, m.bowlingShort));
            binding.score.setText(ctx.getString(R.string.history_score, m.runs, m.wickets, ScoreCalculator.formatOvers(m.balls)));
            binding.result.setText(m.resultText);
            binding.date.setText(Formatters.matchDate(m.playedAt));
            int resultColor = m.won ? R.color.grass_green : m.tied ? R.color.chip_dot : R.color.wicket_red;
            binding.resultStripe.setBackgroundColor(ContextCompat.getColor(ctx, resultColor));
            binding.resultChip.setText(m.won ? R.string.history_won : m.tied ? R.string.history_tied : R.string.history_lost);
            binding.resultChip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, resultColor)));
            boolean daily = GameMode.fromName(m.mode) == GameMode.DAILY_CHALLENGE;
            binding.modeChip.setText(daily ? R.string.mode_daily_short : R.string.mode_quick_short);
            binding.getRoot().setOnClickListener(v -> listener.onMatchClick(m));
        }
    }
}
