package io.github.ieswar23.strikezone.ui.achievements;

import android.content.res.ColorStateList;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.color.MaterialColors;

import io.github.ieswar23.strikezone.R;
import io.github.ieswar23.strikezone.domain.achievements.Achievement;

/** The vector glyph for each badge, drawn on a medal disc: gold on navy when unlocked, muted when locked. */
public final class BadgeIcons {

    private BadgeIcons() {
    }

    @DrawableRes
    public static int iconFor(Achievement achievement) {
        switch (achievement) {
            case FIRST_WIN: return R.drawable.ic_badge_first_win;
            case FIRST_FIFTY: return R.drawable.ic_badge_first_fifty;
            case SIX_MACHINE: return R.drawable.ic_badge_six_machine;
            case PERFECT_TEN: return R.drawable.ic_badge_perfect_ten;
            case DOT_FREE_OVER: return R.drawable.ic_badge_dot_free_over;
            case FLAWLESS_CHASE: return R.drawable.ic_badge_flawless_chase;
            case LAST_BALL_HERO: return R.drawable.ic_badge_last_ball_hero;
            case HARD_MODE_WINNER: return R.drawable.ic_badge_hard_mode_winner;
            case CHASE_MASTER: return R.drawable.ic_badge_chase_master;
            case DAILY_CHALLENGER: return R.drawable.ic_badge_daily_challenger;
            case BIG_HITTER: return R.drawable.ic_badge_big_hitter;
            case BOUNDARY_CENTURY: return R.drawable.ic_badge_boundary_century;
            default: throw new IllegalArgumentException("No icon for " + achievement);
        }
    }

    /** Shows the badge's glyph on its medal in the unlocked (gold) or locked (muted) style. */
    public static void bind(ImageView medal, Achievement achievement, boolean unlocked) {
        medal.setImageResource(iconFor(achievement));
        medal.setBackgroundResource(unlocked ? R.drawable.bg_badge_unlocked : R.drawable.bg_badge_locked);
        int tint = unlocked
                ? ContextCompat.getColor(medal.getContext(), R.color.floodlight_yellow)
                : MaterialColors.getColor(medal, com.google.android.material.R.attr.colorOutline);
        ImageViewCompat.setImageTintList(medal, ColorStateList.valueOf(tint));
    }
}
