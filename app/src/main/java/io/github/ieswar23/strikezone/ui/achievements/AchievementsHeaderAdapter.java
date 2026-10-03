package io.github.ieswar23.strikezone.ui.achievements;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.databinding.ItemAchievementsHeaderBinding;
import io.github.ieswar23.strikezone.domain.achievements.Badge;

/** Single full-width header row: unlock ring, "x of y unlocked" and the next badge to chase. */
public class AchievementsHeaderAdapter extends RecyclerView.Adapter<AchievementsHeaderAdapter.HeaderViewHolder> {
    private AchievementsUiState state;
    private boolean animateRing = true;

    public void submit(AchievementsUiState value) {
        boolean first = state == null;
        state = value;
        if (first) notifyItemInserted(0);
        else notifyItemChanged(0);
    }

    @Override
    public int getItemCount() {
        return state == null ? 0 : 1;
    }

    @NonNull
    @Override
    public HeaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HeaderViewHolder(ItemAchievementsHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull HeaderViewHolder holder, int position) {
        holder.bind(state, animateRing);
        animateRing = false;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemAchievementsHeaderBinding binding;

        HeaderViewHolder(ItemAchievementsHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(AchievementsUiState s, boolean animate) {
            Context ctx = binding.getRoot().getContext();
            binding.unlockedCount.setText(ctx.getString(R.string.achievements_unlocked_count, s.unlocked, s.total));
            binding.unlockRing.setProgressCompat(s.unlockedPercent(), animate);
            Badge next = s.nextUp;
            if (next == null) {
                binding.nextUp.setText(R.string.achievements_all_done);
            } else if (next.getAchievement().isOneOff()) {
                binding.nextUp.setText(ctx.getString(R.string.achievements_next_up_one_off, next.getAchievement().getTitle()));
            } else {
                binding.nextUp.setText(ctx.getString(R.string.achievements_next_up, next.getAchievement().getTitle(),
                        next.getProgress().getProgressText()));
            }
        }
    }
}
