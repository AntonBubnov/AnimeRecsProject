package com.example.animerecsclient.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "anime_recs_session";
    private static final String KEY_TOKEN = "jwt_token";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // 1. Збереження токена (викликаємо після Логіну)
    public void saveToken(String token) {
        editor.putString(KEY_TOKEN, token);
        editor.apply(); // Асинхронне збереження
    }

    // 2. Отримання токена (викликаємо перед запитом)
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null); // null якщо токена немає
    }

    // 3. Очищення (викликаємо при Logout)
    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    // Перевірка чи залогінений користувач
    public boolean isLoggedIn() {
        return getToken() != null;
    }
}