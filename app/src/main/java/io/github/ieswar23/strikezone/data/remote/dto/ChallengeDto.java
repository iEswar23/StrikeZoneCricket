package io.github.ieswar23.strikezone.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ChallengeDto {
    @SerializedName("id")
    public String id;

    @SerializedName("dayIndex")
    public int dayIndex;

    @SerializedName("title")
    public String title;

    @SerializedName("description")
    public String description;

    @SerializedName("overs")
    public int overs;

    @SerializedName("target")
    public int target;

    @SerializedName("difficulty")
    public String difficulty;

    @SerializedName("opponentId")
    public String opponentId;

    @SerializedName("bowlerFocus")
    public String bowlerFocus;

    @SerializedName("badge")
    public String badge;
}
