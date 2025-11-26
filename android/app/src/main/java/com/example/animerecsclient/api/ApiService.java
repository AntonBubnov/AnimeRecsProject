package com.example.animerecsclient.api;

import com.example.animerecsclient.model.LoginRequest;
import com.example.animerecsclient.model.TokenResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // Реєстрація: відправляємо JSON об'єкт
    @POST("register")
    Call<TokenResponse> register(@Body LoginRequest request);

    // Логін: відправляємо Form Data (стандарт OAuth2)
    @FormUrlEncoded
    @POST("token")
    Call<TokenResponse> login(
            @Field("username") String username,
            @Field("password") String password
    );


    // 3. Отримання стрічки з пагінацією 📜
    @retrofit2.http.GET("feed")
    Call<java.util.List<com.example.animerecsclient.model.Anime>> getFeed(
            @Query("page") int page,
            @Query("limit") int limit
    );

    // 4. Отримання аніме для оцінки ⚖️
    @retrofit2.http.GET("evaluate")
    Call<com.example.animerecsclient.model.Anime> getEvaluationAnime();

    // 5. Відправка оцінки 👍👎
    // Створимо ще один клас RateRequest в model, якщо треба, або передамо як об'єкт
    @retrofit2.http.POST("rate")
    Call<Void> rateAnime(@Body com.example.animerecsclient.model.RateRequest request);
}