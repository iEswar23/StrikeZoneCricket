package io.github.ieswar23.strikezone.util;

import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.github.ieswar23.strikezone.data.settings.AppSettings;

/** Haptic feedback that respects the vibration setting; needs no VIBRATE permission. */
@Singleton
public class Haptics {
    private final AppSettings settings;

    @Inject
    public Haptics(AppSettings settings) {
        this.settings = settings;
    }

    public void tick(View view) {
        perform(view, HapticFeedbackConstants.KEYBOARD_TAP);
    }

    public void boundary(View view) {
        perform(view, Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.VIRTUAL_KEY);
    }

    public void wicket(View view) {
        perform(view, Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS);
    }

    private void perform(View view, int constant) {
        if (view != null && settings.isVibrationEnabled()) {
            view.performHapticFeedback(constant);
        }
    }
}
