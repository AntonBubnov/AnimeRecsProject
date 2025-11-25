package com.example.animerecsclient.viewmodel;

import android.app.Application;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.animerecsclient.model.Anime;
import com.example.animerecsclient.repository.AnimeRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EvaluationViewModel extends AndroidViewModel {

    private AnimeRepository repository;

    // LiveData: Fragment підписується на це поле. Коли воно змінюється -> UI оновлюється.
    private MutableLiveData<Anime> currentAnime = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public EvaluationViewModel(@NonNull Application application) {
        super(application);
        repository = new AnimeRepository(application);
        loadNextAnime(); // Завантажуємо перше аніме одразу при старті
    }

    public LiveData<Anime> getCurrentAnime() {
        return currentAnime;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // 1. Завантаження наступного аніме з черги
    public void loadNextAnime() {
        repository.getEvaluationAnime().enqueue(new Callback<Anime>() {
            @Override
            public void onResponse(Call<Anime> call, Response<Anime> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentAnime.setValue(response.body());
                } else {
                    if (response.code() == 404) {
                        errorMessage.setValue("Черга порожня! Ви оцінили все.");
                    } else {
                        errorMessage.setValue("Помилка завантаження: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Anime> call, Throwable t) {
                errorMessage.setValue("Мережева помилка: " + t.getMessage());
            }
        });
    }

    // 2. Відправка оцінки
    public void rateCurrentAnime(String ratingType) {
        Anime anime = currentAnime.getValue();
        if (anime == null) return;

        repository.rateAnime(anime.getId(), ratingType).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Якщо оцінка пройшла успішно -> вантажимо наступне
                    loadNextAnime();
                } else {
                    errorMessage.setValue("Не вдалося зберегти оцінку");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                errorMessage.setValue("Помилка мережі при оцінюванні");
            }
        });
    }
}