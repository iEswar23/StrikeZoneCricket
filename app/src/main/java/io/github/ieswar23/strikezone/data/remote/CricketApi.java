package io.github.ieswar23.strikezone.data.remote;

import io.github.ieswar23.strikezone.data.remote.dto.ChallengesResponse;
import io.github.ieswar23.strikezone.data.remote.dto.TeamsResponse;
import retrofit2.Call;
import retrofit2.http.GET;

/** Tournament API. Served offline by {@link MockInterceptor}. */
public interface CricketApi {
    String BASE_URL = "https://api.strikezone.example/";

    @GET("v1/tournament/teams")
    Call<TeamsResponse> getTeams();

    @GET("v1/challenges/daily")
    Call<ChallengesResponse> getDailyChallenges();
}
