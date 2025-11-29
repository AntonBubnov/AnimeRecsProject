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
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchViewModel extends AndroidViewModel {
    private AnimeRepository repository;
    private MutableLiveData<List<Anime>> searchResults = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private MutableLiveData<AppConstants> constants = new MutableLiveData<>();
    private FilterRequest currentFilter = new FilterRequest();

    // Пагінація
    private int currentPage = 1;
    private boolean isLastPage = false;
    private boolean isAdvancedMode = true; // Чи шукаємо ми через фільтри?
    private String lastQuery = ""; // Останній текстовий запит

    public SearchViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
        loadConstants();
        searchAdvanced(currentFilter, true); // Перше завантаження
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

    // 1. Простий пошук (скидає пагінацію)
    public void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchAdvanced(currentFilter, true); // Повернення до фільтрів
            return;
        }

        isAdvancedMode = false;
        lastQuery = query;
        isLoading.setValue(true);

        // Для простого пошуку (Fuzzy) пагінація не так важлива або реалізується інакше,
        // але тут ми просто вантажимо топ-20.
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

    // 2. Розширений пошук (з підтримкою сторінок)
    public void searchAdvanced(FilterRequest request, boolean resetPage) {
        isAdvancedMode = true;
        this.currentFilter = request;

        if (resetPage) {
            currentPage = 1;
            isLastPage = false;
            request.page = 1;
            // Не очищуємо список миттєво, щоб не було блимання,
            // але після відповіді замінимо його.
        } else {
            if (isLastPage || isLoading.getValue()) return;
            request.page = currentPage;
        }

        isLoading.setValue(true);

        repository.searchAdvanced(request).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Anime> newItems = response.body();
                    List<Anime> currentItems = new ArrayList<>();

                    if (resetPage) {
                        currentItems.addAll(newItems);
                    } else {
                        // Додаємо до існуючих
                        List<Anime> old = searchResults.getValue();
                        if (old != null) currentItems.addAll(old);
                        currentItems.addAll(newItems);
                    }

                    if (newItems.isEmpty() || newItems.size() < request.limit) {
                        isLastPage = true;
                    } else {
                        currentPage++;
                    }

                    searchResults.setValue(currentItems);
                }
            }
            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                isLoading.setValue(false);
            }
        });
    }

    public void loadNextPage() {
        if (isAdvancedMode) {
            searchAdvanced(currentFilter, false);
        }
    }
}