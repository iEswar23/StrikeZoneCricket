package io.github.ieswar23.strikezone.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "teams")
public class TeamEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    @NonNull
    public String name = "";
    @NonNull
    public String shortName = "";
    public String city;
    @NonNull
    public String primaryColor = "#1A2A52";
    @NonNull
    public String secondaryColor = "#FFD23F";
    public String captain;
    public String homeGround;
    public int rating;
    /** Bowling attack serialised as JSON (list of {name, type}). */
    @NonNull
    public String bowlersJson = "[]";
}
