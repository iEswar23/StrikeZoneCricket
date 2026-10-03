package io.github.ieswar23.strikezone.di;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import io.github.ieswar23.strikezone.data.repository.AchievementRepository;
import io.github.ieswar23.strikezone.data.repository.CricketRepository;
import io.github.ieswar23.strikezone.data.repository.DefaultAchievementRepository;
import io.github.ieswar23.strikezone.data.repository.DefaultCricketRepository;
import io.github.ieswar23.strikezone.data.repository.DefaultMatchRepository;
import io.github.ieswar23.strikezone.data.repository.MatchRepository;

@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    abstract CricketRepository bindCricketRepository(DefaultCricketRepository impl);

    @Binds
    abstract MatchRepository bindMatchRepository(DefaultMatchRepository impl);

    @Binds
    abstract AchievementRepository bindAchievementRepository(DefaultAchievementRepository impl);
}
