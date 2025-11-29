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

public class LibraryViewModel extends AndroidViewModel {
    private AnimeRepository repository;

    // Ми використовуємо один ViewModel, але методи будуть викликатися з різними параметрами
    private MutableLiveData<List<Anime>> libraryList = new MutableLiveData<>();

    // Стан пагінації
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private static final int PAGE_SIZE = 50;

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
    }

    public LiveData<List<Anime>> getLibraryList() { return libraryList; }

    // Метод для початкового завантаження (або перезавантаження)
    public void loadLibrary(String status, boolean reset) {
        if (reset) {
            currentPage = 1;
            isLastPage = false;
            // libraryList.setValue(new ArrayList<>()); // Можна очистити, якщо хочете спінер
        }

        if (isLoading || isLastPage) return;

        isLoading = true;
        repository.getLibrary(status, currentPage, PAGE_SIZE).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<Anime> newItems = response.body();
                    List<Anime> currentItems;

                    if (reset) {
                        currentItems = new ArrayList<>();
                    } else {
                        List<Anime> old = libraryList.getValue();
                        currentItems = old != null ? new ArrayList<>(old) : new ArrayList<>();
                    }

                    currentItems.addAll(newItems);

                    if (newItems.size() < PAGE_SIZE) {
                        isLastPage = true;
                    } else {
                        currentPage++;
                    }

                    libraryList.setValue(currentItems);
                }
            }
            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) {
                isLoading = false;
            }
        });
    }
}