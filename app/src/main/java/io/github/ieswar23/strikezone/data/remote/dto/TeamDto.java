package io.github.ieswar23.strikezone.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TeamDto {
    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("shortName")
    public String shortName;

    @SerializedName("city")
    public String city;

    @SerializedName("primaryColor")
    public String primaryColor;

    @SerializedName("secondaryColor")
    public String secondaryColor;

    @SerializedName("captain")
    public String captain;

    @SerializedName("homeGround")
    public String homeGround;

    @SerializedName("rating")
    public int rating;

    @SerializedName("bowlers")
    public List<BowlerDto> bowlers;
}
