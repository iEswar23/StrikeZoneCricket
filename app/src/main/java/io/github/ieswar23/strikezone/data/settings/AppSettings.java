package io.github.ieswar23.strikezone.data.settings;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.github.ieswar23.strikezone.engine.Difficulty;

/** Typed wrapper around the app's SharedPreferences. */
@Singleton
public class AppSettings {
    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    static final String KEY_DIFFICULTY = "default_difficulty";
    static final String KEY_OVERS = "default_overs";
    static final String KEY_VIBRATION = "vibration_enabled";
    static final String KEY_THEME = "theme_mode";
    static final String KEY_FAVOURITE_TEAM = "favourite_team";

    private final SharedPreferences prefs;

    @Inject
    public AppSettings(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public Difficulty getDefaultDifficulty() {
        return Difficulty.fromName(prefs.getString(KEY_DIFFICULTY, null), Difficulty.MEDIUM);
    }

    public void setDefaultDifficulty(Difficulty difficulty) {
        prefs.edit().putString(KEY_DIFFICULTY, difficulty.name()).apply();
    }

    public int getDefaultOvers() {
        return prefs.getInt(KEY_OVERS, 2);
    }

    public void setDefaultOvers(int overs) {
        prefs.edit().putInt(KEY_OVERS, overs).apply();
    }

    public boolean isVibrationEnabled() {
        return prefs.getBoolean(KEY_VIBRATION, true);
    }

    public void setVibrationEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply();
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME, THEME_SYSTEM);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME, mode).apply();
        applyTheme();
    }

    public String getFavouriteTeamId() {
        return prefs.getString(KEY_FAVOURITE_TEAM, null);
    }

    public void setFavouriteTeamId(String teamId) {
        prefs.edit().putString(KEY_FAVOURITE_TEAM, teamId).apply();
    }

    public void applyTheme() {
        int mode;
        switch (getThemeMode()) {
            case THEME_LIGHT:
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case THEME_DARK:
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
