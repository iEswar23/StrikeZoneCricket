package io.github.ieswar23.strikezone.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ChallengesResponse {
    @SerializedName("generatedAt")
    public String generatedAt;

    @SerializedName("challenges")
    public List<ChallengeDto> challenges;
}
