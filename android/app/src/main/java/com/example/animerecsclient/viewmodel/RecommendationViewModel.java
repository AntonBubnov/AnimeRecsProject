package com.example.animerecsclient.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.animerecsclient.model.Anime;
import com.example.animerecsclient.repository.AnimeRepository;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendationViewModel extends AndroidViewModel {

    private AnimeRepository repository;
    private MutableLiveData<List<Anime>> animeList = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public RecommendationViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
        loadFeed(); // Вантажимо одразу при старті
    }

    public LiveData<List<Anime>> getAnimeList() {
        return animeList;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadFeed() {
        repository.getFeed().enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    animeList.setValue(response.body());
                } else {
                    errorMessage.setValue("Error loading feed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }
}