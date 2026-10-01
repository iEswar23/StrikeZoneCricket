package io.github.ieswar23.strikezone;

import android.app.Application;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;
import io.github.ieswar23.strikezone.data.settings.AppSettings;

@HiltAndroidApp
public class StrikeZoneApp extends Application {

    @Inject
    AppSettings settings;

    @Override
    public void onCreate() {
        super.onCreate();
        settings.applyTheme();
    }
}
