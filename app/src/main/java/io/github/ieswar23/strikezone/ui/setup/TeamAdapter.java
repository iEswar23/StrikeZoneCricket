package io.github.ieswar23.strikezone.ui.setup;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.databinding.ItemTeamBinding;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.ui.common.TeamBadge;

/** Horizontal, single-select list of team cards. */
public class TeamAdapter extends ListAdapter<Team, TeamAdapter.TeamViewHolder> {

    public interface OnTeamClick {
        void onTeamClick(Team team);
    }

    private static final DiffUtil.ItemCallback<Team> DIFF = new DiffUtil.ItemCallback<Team>() {
        @Override
        public boolean areItemsTheSame(@NonNull Team a, @NonNull Team b) {
            return a.getId().equals(b.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Team a, @NonNull Team b) {
            return a.equals(b);
        }
    };

    private final OnTeamClick listener;
    private String selectedId;
    private String otherSideId;
    private boolean locked;

    public TeamAdapter(OnTeamClick listener) {
        super(DIFF);
        this.listener = listener;
    }

    /**
     * @param selected   the team selected in this list
     * @param otherSide  the team picked on the other side (shown with a "VS" hint)
     * @param lock       when true, only the selected card is enabled (Daily Challenge opponent)
     */
    public void setSelection(String selected, String otherSide, boolean lock) {
        if (Objects.equals(selected, selectedId) && Objects.equals(otherSide, otherSideId) && lock == locked) return;
        selectedId = selected;
        otherSideId = otherSide;
        locked = lock;
        notifyItemRangeChanged(0, getItemCount());
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TeamViewHolder(ItemTeamBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class TeamViewHolder extends RecyclerView.ViewHolder {
        private final ItemTeamBinding binding;

        TeamViewHolder(ItemTeamBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Team team) {
            boolean selected = team.getId().equals(selectedId);
            boolean enabled = !locked || selected;
            TeamBadge.bind(binding.badge, team.getShortName(), team.getPrimaryColor());
            binding.name.setText(team.getName());
            binding.meta.setText(binding.getRoot().getContext().getString(R.string.setup_team_meta, team.getRating(), team.getCity()));
            binding.getRoot().setChecked(selected);
            binding.getRoot().setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(binding.getRoot().getContext(),
                    selected ? R.color.floodlight_yellow : R.color.transparent)));
            binding.otherSide.setVisibility(team.getId().equals(otherSideId) ? android.view.View.VISIBLE : android.view.View.GONE);
            binding.getRoot().setAlpha(enabled ? (team.getId().equals(otherSideId) ? 0.55f : 1f) : 0.35f);
            binding.getRoot().setEnabled(enabled);
            binding.getRoot().setContentDescription(team.getName() + (selected ? ", selected" : ""));
            binding.getRoot().setOnClickListener(v -> {
                if (enabled) listener.onTeamClick(team);
            });
        }
    }
}
