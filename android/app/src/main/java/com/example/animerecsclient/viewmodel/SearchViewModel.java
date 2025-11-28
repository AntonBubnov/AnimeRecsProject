package com.example.animerecsclient.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.animerecsclient.model.Anime;
import com.example.animerecsclient.model.AppConstants;
import com.example.animerecsclient.model.FilterRequest;
import com.example.animerecsclient.repository.AnimeRepository;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends AndroidViewModel {
    private AnimeRepository repository;
    private MutableLiveData<List<Anime>> searchResults = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Нові поля для фільтрів
    private MutableLiveData<AppConstants> constants = new MutableLiveData<>();
    private FilterRequest currentFilter = new FilterRequest(); // Дефолтний фільтр

    public SearchViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
        loadConstants();
        searchAdvanced(currentFilter); // Початкове завантаження (популярне)
    }

    public LiveData<List<Anime>> getSearchResults() { return searchResults; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<AppConstants> getConstants() { return constants; }
    public FilterRequest getCurrentFilter() { return currentFilter; }

    private void loadConstants() {
        repository.getConstants().enqueue(new Callback<AppConstants>() {
            @Override
            public void onResponse(Call<AppConstants> call, Response<AppConstants> response) {
                if (response.isSuccessful()) constants.setValue(response.body());
            }
            @Override
            public void onFailure(Call<AppConstants> call, Throwable t) {}
        });
    }

    // Простий пошук (по назві)
    public void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Якщо пошук очистили -> повертаємося до фільтрів
            searchAdvanced(currentFilter);
            return;
        }

        isLoading.setValue(true);
        repository.searchAnime(query).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) searchResults.setValue(response.body());
            }
            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                isLoading.setValue(false);
            }
        });
    }

    // Розширений пошук
    public void searchAdvanced(FilterRequest request) {
        this.currentFilter = request; // Запам'ятовуємо
        isLoading.setValue(true);

        repository.searchAdvanced(request).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) searchResults.setValue(response.body());
            }
            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                isLoading.setValue(false);
            }
        });
    }
}