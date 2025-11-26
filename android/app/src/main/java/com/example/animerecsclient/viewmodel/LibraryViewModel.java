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

public class LibraryViewModel extends AndroidViewModel {
    private AnimeRepository repository;

    // Ми використовуємо один ViewModel, але методи будуть викликатися з різними параметрами
    private MutableLiveData<List<Anime>> libraryList = new MutableLiveData<>();

    public LibraryViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
    }

    public LiveData<List<Anime>> getLibraryList() { return libraryList; }

    public void loadLibrary(String status) {
        // Для простоти поки без пагінації (вантажимо перші 100)
        repository.getLibrary(status, 1, 100).enqueue(new Callback<List<Anime>>() {
            @Override
            public void onResponse(Call<List<Anime>> call, Response<List<Anime>> response) {
                if (response.isSuccessful()) {
                    libraryList.setValue(response.body());
                }
            }
            @Override
            public void onFailure(Call<List<Anime>> call, Throwable t) { }
        });
    }
}