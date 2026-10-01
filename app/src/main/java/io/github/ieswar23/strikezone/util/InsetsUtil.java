package io.github.ieswar23.strikezone.util;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/** Edge-to-edge helpers: pad views by the system bar insets while keeping their own padding. */
public final class InsetsUtil {

    private InsetsUtil() {
    }

    public static void padTop(View view) {
        apply(view, true, false);
    }

    public static void padBottom(View view) {
        apply(view, false, true);
    }

    public static void padTopAndBottom(View view) {
        apply(view, true, true);
    }

    private static void apply(View view, boolean top, boolean bottom) {
        final int left = view.getPaddingLeft();
        final int baseTop = view.getPaddingTop();
        final int right = view.getPaddingRight();
        final int baseBottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(left + bars.left,
                    baseTop + (top ? bars.top : 0),
                    right + bars.right,
                    baseBottom + (bottom ? bars.bottom : 0));
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
