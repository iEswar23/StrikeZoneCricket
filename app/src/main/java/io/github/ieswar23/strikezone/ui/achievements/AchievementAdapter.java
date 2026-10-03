package io.github.ieswar23.strikezone.ui.achievements;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.util.Objects;

import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.databinding.ItemAchievementBinding;
import io.github.ieswar23.strikezone.domain.achievements.Achievement;
import io.github.ieswar23.strikezone.domain.achievements.AchievementProgress;
import io.github.ieswar23.strikezone.domain.achievements.Badge;
import io.github.ieswar23.strikezone.util.Formatters;

/** Grid of badge tiles: unlocked ones in colour with their date, locked ones greyed with progress. */
public class AchievementAdapter extends ListAdapter<Badge, AchievementAdapter.BadgeViewHolder> {

    private static final DiffUtil.ItemCallback<Badge> DIFF = new DiffUtil.ItemCallback<Badge>() {
        @Override
        public boolean areItemsTheSame(@NonNull Badge a, @NonNull Badge b) {
            return a.getAchievement() == b.getAchievement();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Badge a, @NonNull Badge b) {
            return Objects.equals(a.getUnlockedAt(), b.getUnlockedAt())
                    && a.getProgress().getCurrent() == b.getProgress().getCurrent();
        }
    };

    public AchievementAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BadgeViewHolder(ItemAchievementBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class BadgeViewHolder extends RecyclerView.ViewHolder {
        private final ItemAchievementBinding binding;

        BadgeViewHolder(ItemAchievementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Badge badge) {
            Context ctx = binding.getRoot().getContext();
            Achievement a = badge.getAchievement();
            AchievementProgress p = badge.getProgress();
            boolean unlocked = badge.isUnlocked();
            float density = ctx.getResources().getDisplayMetrics().density;

            binding.title.setText(a.getTitle());
            binding.description.setText(a.getDescription());

            BadgeIcons.bind(binding.medal, a, unlocked);
            binding.lockIcon.setVisibility(unlocked ? View.GONE : View.VISIBLE);
            binding.title.setTextColor(MaterialColors.getColor(binding.title,
                    unlocked ? com.google.android.material.R.attr.colorOnSurface
                            : com.google.android.material.R.attr.colorOnSurfaceVariant));

            binding.card.setCardBackgroundColor(MaterialColors.getColor(binding.card,
                    unlocked ? com.google.android.material.R.attr.colorSurfaceContainerLowest
                            : com.google.android.material.R.attr.colorSurfaceContainer));
            binding.card.setCardElevation(unlocked ? 2 * density : 0f);
            binding.card.setStrokeWidth(Math.round((unlocked ? 1.5f : 0f) * density));
            binding.card.setStrokeColor(ContextCompat.getColor(ctx, R.color.floodlight_yellow));

            binding.unlockedDate.setVisibility(unlocked ? View.VISIBLE : View.GONE);
            binding.progressGroup.setVisibility(unlocked ? View.GONE : View.VISIBLE);
            String status;
            if (unlocked) {
                String date = Formatters.shortDate(badge.getUnlockedAt());
                binding.unlockedDate.setText(ctx.getString(R.string.achievements_unlocked_on, date));
                status = ctx.getString(R.string.achievements_cd_unlocked, a.getTitle(), date, a.getDescription());
            } else {
                binding.progress.setProgressCompat(p.getPercent(), false);
                String counter = a.isOneOff() ? ctx.getString(R.string.achievements_locked) : p.getProgressText();
                binding.progressText.setText(counter);
                status = ctx.getString(R.string.achievements_cd_locked, a.getTitle(), counter, a.getDescription());
            }
            binding.getRoot().setContentDescription(status);
        }
    }
}
