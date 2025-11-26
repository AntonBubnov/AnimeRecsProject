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

public class SearchViewModel extends AndroidViewModel {
    private AnimeRepository repository;
    private MutableLiveData<List<Anime>> searchResults = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public SearchViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
    }

    public LiveData<List<Anime>> getSearchResults() { return searchResults; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchResults.setValue(null); // Очищуємо список
            return;
        }

        isLoading.setValue(true);
        repository.searchAnime(query).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    searchResults.setValue(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                isLoading.setValue(false);
            }
        });
    }
}