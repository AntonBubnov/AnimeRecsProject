package com.example.animerecsclient.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.animerecsclient.R;
import com.example.animerecsclient.api.RetrofitClient;
import com.example.animerecsclient.model.TokenResponse;
import com.example.animerecsclient.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Знаходимо Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);

        // 2. Встановлюємо його як системний Action Bar
        setSupportActionBar(toolbar);

        // 3. Тепер цей код спрацює і покаже стрілку!
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); // Прибираємо заголовок
        }

        sessionManager = new SessionManager(this);

        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnDoLogin);

        btnLogin.setOnClickListener(v -> performLogin());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Це стандартна функція Android "повернутися назад"
        return true;
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Виклик API
        RetrofitClient.getInstance(this).getApi().login(username, password)
                .enqueue(new Callback<TokenResponse>() {
                    @Override
                    public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // 1. Зберігаємо токен
                            String token = response.body().getAccessToken();
                            sessionManager.saveToken(token);

                            // 2. Переходимо до головного екрану
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            // Очищуємо стек, щоб кнопка Back не повернула на логін
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TokenResponse> call, Throwable t) {
                        Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}