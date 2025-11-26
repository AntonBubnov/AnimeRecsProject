package com.example.animerecsclient.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.animerecsclient.R;
import com.example.animerecsclient.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        // Кнопка Logout (вже була)
        ImageButton btnLogout = findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> logoutUser());

        // Налаштування навігації
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_evaluate) {
                selectedFragment = new EvaluationFragment();
            } else if (itemId == R.id.nav_feed) {
                selectedFragment = new RecommendationFragment();
            } else if (itemId == R.id.nav_library) {
                selectedFragment = new LibraryFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Завантажуємо перший фрагмент при старті (Evaluation)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EvaluationFragment())
                    .commit();
            // Виділяємо правильну кнопку в меню
            bottomNav.setSelectedItemId(R.id.nav_evaluate);
        }
    }

    private void logoutUser() {
        sessionManager.clearSession();
        Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}