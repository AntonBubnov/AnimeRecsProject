package com.example.animerecsclient.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.animerecsclient.R;
import com.example.animerecsclient.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);

        // Затримка 2 секунди для відображення логотипу
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkTokenAndNavigate();
            }
        }, 2000);
    }

    private void checkTokenAndNavigate() {
        String token = sessionManager.getToken();
        Intent intent;

        if (token != null && !token.isEmpty()) {
            // Токен є -> Головний екран (EvaluationFragment буде першим)
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // Токена немає -> Екран вітання
            intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        }

        startActivity(intent);
        finish(); // Закриваємо Splash, щоб не можна було повернутися
    }
}