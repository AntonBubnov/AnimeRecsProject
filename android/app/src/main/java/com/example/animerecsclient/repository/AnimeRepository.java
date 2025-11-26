package com.example.animerecsclient.repository;

import android.content.Context;
import com.example.animerecsclient.api.ApiService;
import com.example.animerecsclient.api.RetrofitClient;
import com.example.animerecsclient.model.Anime;
import com.example.animerecsclient.model.RateRequest;
import retrofit2.Call;

public class AnimeRepository {

    private ApiService apiService;

    public AnimeRepository(Context context) {
        // Отримуємо екземпляр API
        apiService = RetrofitClient.getInstance(context).getApi();
    }

    // Отримання аніме для оцінки
    public Call<Anime> getEvaluationAnime() {
        return apiService.getEvaluationAnime();
    }

    // Відправка оцінки
    public Call<Void> rateAnime(int animeId, String ratingType) {
        return apiService.rateAnime(new RateRequest(animeId, ratingType));
    }

    public Call<java.util.List<Anime>> getFeed(int page, int limit) {
        return apiService.getFeed(page, limit);
    }

    public Call<com.example.animerecsclient.model.AnimeDetails> getAnimeDetails(int id) {
        return apiService.getAnimeDetails(id);
    }

    public Call<java.util.List<Anime>> getLibrary(String status, int page, int limit) {
        return apiService.getLibrary(status, page, limit);
    }
}