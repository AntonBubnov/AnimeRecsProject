package com.example.animerecsclient.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.animerecsclient.model.AnimeDetails;
import com.example.animerecsclient.repository.AnimeRepository;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnimeDetailViewModel extends AndroidViewModel {
    private AnimeRepository repository;
    private MutableLiveData<AnimeDetails> animeDetails = new MutableLiveData<>();

    public AnimeDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
    }

    public LiveData<AnimeDetails> getAnimeDetails() { return animeDetails; }

    public void loadDetails(int animeId) {
        repository.getAnimeDetails(animeId).enqueue(new Callback<AnimeDetails>() {
            @Override
            public void onResponse(Call<AnimeDetails> call, Response<AnimeDetails> response) {
                if (response.isSuccessful()) animeDetails.setValue(response.body());
            }
            @Override
            public void onFailure(Call<AnimeDetails> call, Throwable t) { /* Handle error */ }
        });
    }

    public void updateStatus(int animeId, String status) {
        repository.rateAnime(animeId, status).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if(response.isSuccessful()) loadDetails(animeId); // Оновлюємо дані
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}