package io.github.ieswar23.strikezone.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TeamsResponse {
    @SerializedName("tournament")
    public String tournament;

    @SerializedName("season")
    public String season;

    @SerializedName("teams")
    public List<TeamDto> teams;
}
