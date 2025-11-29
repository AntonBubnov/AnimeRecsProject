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

    // Дані для Вітрини (Dashboard)
    private MutableLiveData<List<Anime>> popularList = new MutableLiveData<>();
    private MutableLiveData<List<Anime>> rankList = new MutableLiveData<>();
    private MutableLiveData<List<Anime>> newList = new MutableLiveData<>();

    // Дані для Сітки (Grid Mode)
    private MutableLiveData<List<Anime>> gridList = new MutableLiveData<>();

    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Стан пагінації для сітки
    private int currentPage = 1;
    private boolean isLastPage = false;
    private String currentSortBy = "popularity";

    public RecommendationViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
        loadDashboard(); // Вантажимо вітрину при старті
    }

    // Getters
    public LiveData<List<Anime>> getPopularList() { return popularList; }
    public LiveData<List<Anime>> getRankList() { return rankList; }
    public LiveData<List<Anime>> getNewList() { return newList; }
    public LiveData<List<Anime>> getGridList() { return gridList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // Завантаження всіх каруселей (паралельно)
    public void loadDashboard() {
        isLoading.setValue(true);
        fetchCarousel("popularity", popularList);
        fetchCarousel("rank", rankList);
        fetchCarousel("year", newList);
    }

    private void fetchCarousel(String sortBy, MutableLiveData<List<Anime>> target) {
        // Беремо тільки перші 10 штук для каруселі
        repository.getFeed(sortBy, 1, 10).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                if (response.isSuccessful()) target.setValue(response.body());
                isLoading.setValue(false); // Спрощено (вимикаємо спінер, коли хоча б один завантажився)
            }
            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                isLoading.setValue(false);
            }
        });
    }

    // Вхід у режим сітки (скидання та завантаження першої сторінки)
    public void openGrid(String sortBy) {
        currentSortBy = sortBy;
        currentPage = 1;
        isLastPage = false;
        gridList.setValue(new ArrayList<>()); // Очистка
        loadGridFirstPage();
    }

    // Пагінація для сітки
    public void loadNextGridPage() {
        if (isLoading.getValue() || isLastPage) return;

        isLoading.setValue(true);
        repository.getFeed(currentSortBy, currentPage, 50).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Anime> newItems = response.body();
                    if (newItems.isEmpty()) {
                        isLastPage = true;
                        return;
                    }

                    List<Anime> currentItems = gridList.getValue();
                    if (currentItems == null) currentItems = new ArrayList<>();
                    currentItems.addAll(newItems);
                    gridList.setValue(currentItems);

                    currentPage++;
                }
            }

            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue(t.getMessage());
            }
        });
    }

    // Оновлення поточного списку сітки (викликається з onResume)
    public void refreshGrid() {
        if (currentSortBy == null) return;

        // Скидаємо стан пагінації
        currentPage = 1;
        isLastPage = false;

        loadGridFirstPage();
    }

    private void loadGridFirstPage() {
        isLoading.setValue(true);
        repository.getFeed(currentSortBy, 1, 50).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    // ТУТ ВІДМІННІСТЬ: Ми ПЕРЕЗАПИСУЄМО список, а не додаємо
                    gridList.setValue(response.body());
                    currentPage = 2; // Наступна сторінка буде 2
                    isLastPage = response.body().isEmpty();
                }
            }
            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                isLoading.setValue(false);
            }
        });
    }
}