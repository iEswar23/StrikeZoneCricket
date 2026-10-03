package io.github.ieswar23.strikezone.data.local;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Collections;
import java.util.List;

import io.github.ieswar23.strikezone.data.local.entity.AchievementEntity;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;

/**
 * Opens a database written by schema v1 (before achievements) with the current {@link AppDatabase}
 * and checks that {@link AppDatabase#MIGRATION_1_2} keeps the match history and passes Room's
 * schema validation (no destructive fallback is configured here, so a bad migration throws).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
public class AppDatabaseMigrationTest {
    private static final String NAME = "migration-test.db";

    /** The v1 schema exactly as Room generated it for version 1. */
    private static final String[] V1_SCHEMA = {
            "CREATE TABLE IF NOT EXISTS `teams` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `shortName` TEXT NOT NULL, "
                    + "`city` TEXT, `primaryColor` TEXT NOT NULL, `secondaryColor` TEXT NOT NULL, `captain` TEXT, "
                    + "`homeGround` TEXT, `rating` INTEGER NOT NULL, `bowlersJson` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `challenges` (`id` TEXT NOT NULL, `dayIndex` INTEGER NOT NULL, `title` TEXT NOT NULL, "
                    + "`description` TEXT, `overs` INTEGER NOT NULL, `target` INTEGER NOT NULL, `difficulty` TEXT NOT NULL, "
                    + "`opponentId` TEXT NOT NULL, `bowlerFocus` TEXT, `badge` TEXT, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `matches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playedAt` INTEGER NOT NULL, "
                    + "`mode` TEXT NOT NULL, `challengeId` TEXT, `bowlerFocus` TEXT, `battingTeamId` TEXT NOT NULL, "
                    + "`battingTeamName` TEXT NOT NULL, `battingShort` TEXT NOT NULL, `battingColor` TEXT NOT NULL, "
                    + "`bowlingTeamId` TEXT NOT NULL, `bowlingTeamName` TEXT NOT NULL, `bowlingShort` TEXT NOT NULL, "
                    + "`bowlingColor` TEXT NOT NULL, `overs` INTEGER NOT NULL, `wicketsLimit` INTEGER NOT NULL, "
                    + "`target` INTEGER NOT NULL, `difficulty` TEXT NOT NULL, `runs` INTEGER NOT NULL, `wickets` INTEGER NOT NULL, "
                    + "`balls` INTEGER NOT NULL, `fours` INTEGER NOT NULL, `sixes` INTEGER NOT NULL, `dots` INTEGER NOT NULL, "
                    + "`extras` INTEGER NOT NULL, `won` INTEGER NOT NULL, `tied` INTEGER NOT NULL, `resultText` TEXT NOT NULL, "
                    + "`overSummary` TEXT NOT NULL)",
            "CREATE INDEX IF NOT EXISTS `index_matches_playedAt` ON `matches` (`playedAt`)",
            "CREATE INDEX IF NOT EXISTS `index_matches_challengeId` ON `matches` (`challengeId`)",
    };

    private Context context;
    private AppDatabase database;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(NAME);
    }

    @After
    public void tearDown() {
        if (database != null) database.close();
        context.deleteDatabase(NAME);
    }

    @Test
    public void migratingFromV1KeepsHistoryAndAddsAchievements() {
        SQLiteDatabase v1 = context.openOrCreateDatabase(NAME, Context.MODE_PRIVATE, null);
        for (String sql : V1_SCHEMA) v1.execSQL(sql);
        v1.execSQL("INSERT INTO matches (playedAt, mode, challengeId, bowlerFocus, battingTeamId, battingTeamName, "
                + "battingShort, battingColor, bowlingTeamId, bowlingTeamName, bowlingShort, bowlingColor, overs, "
                + "wicketsLimit, target, difficulty, runs, wickets, balls, fours, sixes, dots, extras, won, tied, "
                + "resultText, overSummary) VALUES (1759000000000, 'DAILY_CHALLENGE', 'dc-002', 'SPIN', 'mum', "
                + "'Mumbai Mavericks', 'MUM', '#1E6FD9', 'col', 'Colombo Cyclones', 'COL', '#0F4C81', 2, 3, 24, "
                + "'MEDIUM', 25, 1, 10, 2, 1, 2, 1, 1, 0, 'Mumbai Mavericks won by 2 wickets', '14:0,11:1')");
        v1.setVersion(1);
        v1.close();

        database = Room.databaseBuilder(context, AppDatabase.class, NAME)
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .allowMainThreadQueries()
                .build();

        List<MatchEntity> matches = database.matchDao().getAllSync();
        assertThat(matches).hasSize(1);
        MatchEntity m = matches.get(0);
        assertThat(m.battingTeamName).isEqualTo("Mumbai Mavericks");
        assertThat(m.challengeId).isEqualTo("dc-002");
        assertThat(m.runs).isEqualTo(25);
        assertThat(m.won).isTrue();
        assertThat(m.overSummary).isEqualTo("14:0,11:1");
        // Shot facts didn't exist in v1, so older matches start from zero.
        assertThat(m.perfectShots).isEqualTo(0);
        assertThat(m.maxSixesInOver).isEqualTo(0);
        assertThat(m.scoringOvers).isEqualTo(0);

        database.achievementDao().insertAll(Collections.singletonList(new AchievementEntity("FIRST_WIN", 42L, m.id)));
        database.achievementDao().insertAll(Collections.singletonList(new AchievementEntity("FIRST_WIN", 99L, m.id)));
        List<AchievementEntity> unlocked = database.achievementDao().getAllSync();
        assertThat(unlocked).hasSize(1);
        assertThat(unlocked.get(0).unlockedAt).isEqualTo(42L);
    }
}
