package io.github.ieswar23.strikezone.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class BowlerDto {
    @SerializedName("name")
    public String name;

    @SerializedName("type")
    public String type;

    public BowlerDto() {
    }

    public BowlerDto(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
