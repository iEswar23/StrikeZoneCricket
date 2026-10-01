package io.github.ieswar23.strikezone.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import io.github.ieswar23.strikezone.data.local.dao.ChallengeDao;
import io.github.ieswar23.strikezone.data.local.dao.MatchDao;
import io.github.ieswar23.strikezone.data.local.dao.TeamDao;
import io.github.ieswar23.strikezone.data.local.entity.ChallengeEntity;
import io.github.ieswar23.strikezone.data.local.entity.MatchEntity;
import io.github.ieswar23.strikezone.data.local.entity.TeamEntity;

@Database(entities = {TeamEntity.class, ChallengeEntity.class, MatchEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public static final String NAME = "strikezone.db";

    public abstract TeamDao teamDao();

    public abstract ChallengeDao challengeDao();

    public abstract MatchDao matchDao();
}
