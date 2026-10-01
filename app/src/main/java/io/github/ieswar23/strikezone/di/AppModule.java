package io.github.ieswar23.strikezone.di;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.github.ieswar23.strikezone.data.repository.CatalogMapper;
import io.github.ieswar23.strikezone.util.AppExecutors;

@Module
@InstallIn(SingletonComponent.class)
public final class AppModule {

    private AppModule() {
    }

    @Provides
    @Singleton
    static AppExecutors provideExecutors() {
        return AppExecutors.create();
    }

    @Provides
    @Singleton
    static SharedPreferences providePreferences(@ApplicationContext Context context) {
        return context.getSharedPreferences("strikezone_settings", Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    static Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    static CatalogMapper provideCatalogMapper(Gson gson) {
        return new CatalogMapper(gson);
    }
}
