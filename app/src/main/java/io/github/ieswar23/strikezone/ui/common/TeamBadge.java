package io.github.ieswar23.strikezone.ui.common;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import io.github.ieswar23.strikezone.util.ColorUtils;

/** Renders a team's short code inside a circular badge filled with the team colour. */
public final class TeamBadge {

    private TeamBadge() {
    }

    public static void bind(TextView view, String shortName, String colorHex) {
        int color = ColorUtils.parse(colorHex, Color.parseColor("#1A2A52"));
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        circle.setStroke(Math.round(2 * view.getResources().getDisplayMetrics().density),
                ColorUtils.withAlpha(Color.WHITE, 90));
        view.setBackground(circle);
        view.setText(shortName);
        view.setTextColor(ColorUtils.isDark(color) ? Color.WHITE : Color.parseColor("#0A1128"));
    }
}
