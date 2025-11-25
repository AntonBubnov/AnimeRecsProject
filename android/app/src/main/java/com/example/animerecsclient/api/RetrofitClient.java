package com.example.animerecsclient.api;

import android.content.Context;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Старе (Емулятор):
    // private static final String BASE_URL = "http://10.0.2.2:8000/";

    // Нове (Production):
    private static final String BASE_URL = "https://whatsnext-api.onrender.com/";
    private static RetrofitClient instance;
    private Retrofit retrofit;

    // Приватний конструктор тепер приймає Context
    private RetrofitClient(Context context) {

        // Створюємо OkHttpClient з нашим перехоплювачем
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context))
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client) // <--- ПІДКЛЮЧАЄМО ЙОГО ТУТ
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context);
        }
        return instance;
    }

    public ApiService getApi() {
        return retrofit.create(ApiService.class);
    }
}