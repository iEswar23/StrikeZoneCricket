package io.github.ieswar23.strikezone.di;

import android.content.Context;

import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import io.github.ieswar23.strikezone.data.remote.CricketApi;
import io.github.ieswar23.strikezone.data.remote.MockInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public final class NetworkModule {

    private NetworkModule() {
    }

    @Provides
    @Singleton
    static OkHttpClient provideOkHttp(@ApplicationContext Context context) {
        return new OkHttpClient.Builder()
                .addInterceptor(new MockInterceptor(context.getAssets(), 300, 700))
                .callTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    static Retrofit provideRetrofit(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(CricketApi.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    static CricketApi provideApi(Retrofit retrofit) {
        return retrofit.create(CricketApi.class);
    }
}
