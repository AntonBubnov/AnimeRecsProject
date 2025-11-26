package com.example.animerecsclient.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.animerecsclient.model.Anime;
import com.example.animerecsclient.repository.AnimeRepository;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendationViewModel extends AndroidViewModel {

    private AnimeRepository repository;
    private MutableLiveData<List<Anime>> animeList = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Змінні для пагінації
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int PAGE_SIZE = 50;

    public RecommendationViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
        loadFeed(true); // Завантажуємо першу сторінку
    }

    public LiveData<List<Anime>> getAnimeList() {
        return animeList;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Метод для завантаження даних
    public void loadFeed(boolean isFirstPage) {
        if (isLoading || (isLastPage && !isFirstPage)) return;

        isLoading = true;

        if (isFirstPage) {
            currentPage = 1;
            isLastPage = false;
        }

        repository.getFeed(currentPage, PAGE_SIZE).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<Anime> newItems = response.body();

                    if (newItems.isEmpty()) {
                        isLastPage = true;
                        return;
                    }

                    List<Anime> currentItems;
                    if (isFirstPage) {
                        currentItems = new ArrayList<>();
                    } else {
                        // Беремо поточний список, щоб додати до нього нові
                        List<Anime> oldItems = animeList.getValue();
                        currentItems = oldItems != null ? new ArrayList<>(oldItems) : new ArrayList<>();
                    }

                    currentItems.addAll(newItems);
                    animeList.setValue(currentItems); // Оновлюємо LiveData

                    // Готуємося до наступної сторінки
                    currentPage++;
                } else {
                    errorMessage.setValue("Error loading feed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                isLoading = false;
                errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }

    // Публічний метод для виклику з UI
    public void loadNextPage() {
        loadFeed(false);
    }
}