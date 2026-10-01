package io.github.ieswar23.strikezone.util;

import android.graphics.Color;

/** Team colour helpers. */
public final class ColorUtils {

    private ColorUtils() {
    }

    public static int parse(String hex, int fallback) {
        if (hex == null) return fallback;
        try {
            return Color.parseColor(hex);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** True when white text reads better than dark text on the given background. */
    public static boolean isDark(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance < 0.6;
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /** Linear blend between two colours (t = 0 → a, t = 1 → b). */
    public static int blend(int a, int b, float t) {
        float u = 1f - t;
        return Color.argb(
                Math.round(Color.alpha(a) * u + Color.alpha(b) * t),
                Math.round(Color.red(a) * u + Color.red(b) * t),
                Math.round(Color.green(a) * u + Color.green(b) * t),
                Math.round(Color.blue(a) * u + Color.blue(b) * t));
    }
}
