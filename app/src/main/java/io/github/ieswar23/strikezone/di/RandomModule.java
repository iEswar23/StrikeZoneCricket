package io.github.ieswar23.strikezone.di;

import java.util.Random;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Source of randomness for the game engine (deliveries, field plans, shot resolution). Kept in its
 * own module so tests can swap in a seeded {@link Random} with {@code @UninstallModules}.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class RandomModule {

    private RandomModule() {
    }

    /** Fresh, time-seeded randomness for each game. */
    @Provides
    static Random provideRandom() {
        return new Random();
    }
}
