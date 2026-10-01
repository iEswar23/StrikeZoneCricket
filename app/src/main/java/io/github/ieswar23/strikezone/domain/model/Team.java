package io.github.ieswar23.strikezone.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.github.ieswar23.strikezone.engine.Bowler;

/** A franchise from the tournament catalogue. Colours are kept as hex strings (UI parses them). */
public final class Team {
    private final String id;
    private final String name;
    private final String shortName;
    private final String city;
    private final String primaryColor;
    private final String secondaryColor;
    private final String captain;
    private final String homeGround;
    private final int rating;
    private final List<Bowler> bowlers;

    public Team(String id, String name, String shortName, String city, String primaryColor, String secondaryColor,
                String captain, String homeGround, int rating, List<Bowler> bowlers) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.city = city;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.captain = captain;
        this.homeGround = homeGround;
        this.rating = rating;
        this.bowlers = bowlers == null ? Collections.emptyList() : Collections.unmodifiableList(bowlers);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getCity() {
        return city;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public String getCaptain() {
        return captain;
    }

    public String getHomeGround() {
        return homeGround;
    }

    public int getRating() {
        return rating;
    }

    public List<Bowler> getBowlers() {
        return bowlers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team)) return false;
        Team team = (Team) o;
        return rating == team.rating && id.equals(team.id) && name.equals(team.name)
                && shortName.equals(team.shortName) && primaryColor.equals(team.primaryColor)
                && Objects.equals(captain, team.captain);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, shortName, primaryColor, rating);
    }
}
