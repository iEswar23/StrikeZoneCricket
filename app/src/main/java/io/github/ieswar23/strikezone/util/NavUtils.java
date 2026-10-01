package io.github.ieswar23.strikezone.util;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

/** Navigation helpers. */
public final class NavUtils {

    private NavUtils() {
    }

    /**
     * Navigates only if the action exists on the current destination. Prevents the classic
     * "action not found" crash when a button is double-tapped during a transition.
     */
    public static void navigateSafely(NavController nav, int actionId, @Nullable Bundle args) {
        NavDestination current = nav.getCurrentDestination();
        if (current != null && current.getAction(actionId) != null) {
            nav.navigate(actionId, args);
        }
    }
}
