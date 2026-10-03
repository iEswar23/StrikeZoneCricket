package io.github.ieswar23.strikezone.data.local;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import io.github.ieswar23.strikezone.data.local.dao.AchievementDao;
import io.github.ieswar23.strikezone.data.local.dao.ChallengeDao;
import io.github.ieswar23.strikezone.data.local.dao.MatchDao;
import io.github.ieswar23.strikezone.data.local.dao.TeamDao;
import io.github.ieswar23.strikezone.data.local.entity.AchievementEntity;
import io.github.ieswar23.strikezone.data.local.entity.ChallengeEntity;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.data.local.entity.TeamEntity;

@Database(entities = {TeamEntity.class, ChallengeEntity.class, MatchEntity.class, AchievementEntity.class},
        version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public static final String NAME = "strikezone.db";

    /**
     * v2 (Achievements): per-match shot facts on {@code matches} and the {@code achievements} table.
     * Existing history is kept; the new columns default to 0 for matches played before the upgrade.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `matches` ADD COLUMN `perfectShots` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `matches` ADD COLUMN `maxSixesInOver` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE `matches` ADD COLUMN `scoringOvers` INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE TABLE IF NOT EXISTS `achievements` (`id` TEXT NOT NULL, `unlockedAt` INTEGER NOT NULL, "
                    + "`matchId` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievements_matchId` ON `achievements` (`matchId`)");
        }
    };

    public abstract TeamDao teamDao();

    public abstract ChallengeDao challengeDao();

    public abstract MatchDao matchDao();

    public abstract AchievementDao achievementDao();
}
