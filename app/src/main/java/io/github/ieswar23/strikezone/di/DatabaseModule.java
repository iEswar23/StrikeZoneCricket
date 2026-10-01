package io.github.ieswar23.strikezone.di;

import android.content.Context;

import androidx.room.Room;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.github.ieswar23.strikezone.data.local.AppDatabase;
import io.github.ieswar23.strikezone.data.local.dao.ChallengeDao;
import io.github.ieswar23.strikezone.data.local.dao.MatchDao;
import io.github.ieswar23.strikezone.data.local.dao.TeamDao;

@Module
@InstallIn(SingletonComponent.class)
public final class DatabaseModule {

    private DatabaseModule() {
    }

    @Provides
    @Singleton
    static AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, AppDatabase.NAME)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Provides
    static TeamDao provideTeamDao(AppDatabase db) {
        return db.teamDao();
    }

    @Provides
    static ChallengeDao provideChallengeDao(AppDatabase db) {
        return db.challengeDao();
    }

    @Provides
    static MatchDao provideMatchDao(AppDatabase db) {
        return db.matchDao();
    }
}
