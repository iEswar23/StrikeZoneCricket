package io.github.ieswar23.strikezone.data.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.github.ieswar23.strikezone.data.local.entity.ChallengeEntity;
import io.github.ieswar23.strikezone.data.local.entity.TeamEntity;
import io.github.ieswar23.strikezone.data.remote.dto.BowlerDto;
import io.github.ieswar23.strikezone.data.remote.dto.ChallengeDto;
import io.github.ieswar23.strikezone.data.remote.dto.TeamDto;
import io.github.ieswar23.strikezone.domain.model.DailyChallenge;
import io.github.ieswar23.strikezone.domain.model.Team;
import io.github.ieswar23.strikezone.engine.Bowler;
import io.github.ieswar23.strikezone.engine.BowlerType;
import io.github.ieswar23.strikezone.engine.Difficulty;

/** Converts between API DTOs, Room entities and domain models. */
public final class CatalogMapper {
    private static final Type BOWLER_LIST = new TypeToken<List<BowlerDto>>() { }.getType();

    private final Gson gson;

    public CatalogMapper(Gson gson) {
        this.gson = gson;
    }

    public TeamEntity toEntity(TeamDto dto) {
        TeamEntity e = new TeamEntity();
        e.id = dto.id;
        e.name = dto.name;
        e.shortName = dto.shortName;
        e.city = dto.city;
        e.primaryColor = dto.primaryColor;
        e.secondaryColor = dto.secondaryColor;
        e.captain = dto.captain;
        e.homeGround = dto.homeGround;
        e.rating = dto.rating;
        e.bowlersJson = gson.toJson(dto.bowlers == null ? new ArrayList<BowlerDto>() : dto.bowlers, BOWLER_LIST);
        return e;
    }

    public Team toDomain(TeamEntity e) {
        List<BowlerDto> dtos = gson.fromJson(e.bowlersJson, BOWLER_LIST);
        List<Bowler> bowlers = new ArrayList<>();
        if (dtos != null) {
            for (BowlerDto b : dtos) {
                bowlers.add(new Bowler(b.name, BowlerType.fromName(b.type, BowlerType.MEDIUM)));
            }
        }
        return new Team(e.id, e.name, e.shortName, e.city, e.primaryColor, e.secondaryColor, e.captain,
                e.homeGround, e.rating, bowlers);
    }

    public List<Team> toTeams(List<TeamEntity> entities) {
        List<Team> teams = new ArrayList<>();
        if (entities != null) {
            for (TeamEntity e : entities) teams.add(toDomain(e));
        }
        return teams;
    }

    public ChallengeEntity toEntity(ChallengeDto dto) {
        ChallengeEntity e = new ChallengeEntity();
        e.id = dto.id;
        e.dayIndex = dto.dayIndex;
        e.title = dto.title;
        e.description = dto.description;
        e.overs = dto.overs;
        e.target = dto.target;
        e.difficulty = dto.difficulty == null ? Difficulty.MEDIUM.name() : dto.difficulty;
        e.opponentId = dto.opponentId;
        e.bowlerFocus = dto.bowlerFocus;
        e.badge = dto.badge;
        return e;
    }

    public DailyChallenge toDomain(ChallengeEntity e) {
        return new DailyChallenge(e.id, e.dayIndex, e.title, e.description, e.overs, e.target,
                Difficulty.fromName(e.difficulty, Difficulty.MEDIUM), e.opponentId, e.bowlerFocus, e.badge);
    }

    public List<DailyChallenge> toChallenges(List<ChallengeEntity> entities) {
        List<DailyChallenge> list = new ArrayList<>();
        if (entities != null) {
            for (ChallengeEntity e : entities) list.add(toDomain(e));
        }
        return list;
    }
}
