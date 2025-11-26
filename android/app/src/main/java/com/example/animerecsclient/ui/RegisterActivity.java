package com.example.animerecsclient.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.animerecsclient.R;
import com.example.animerecsclient.api.RetrofitClient;
import com.example.animerecsclient.model.LoginRequest;
import com.example.animerecsclient.model.TokenResponse;
import com.example.animerecsclient.utils.SessionManager;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etPassword, etRepeatPassword;
    private Button btnRegister;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Знаходимо Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);

        // 2. Встановлюємо його як системний Action Bar
        setSupportActionBar(toolbar);

        // 3. Тепер цей код спрацює і покаже стрілку!
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); // Прибираємо заголовок
        }

        // Обробка натискання на фон
        findViewById(R.id.rootLayout).setOnTouchListener((v, event) -> {
            hideKeyboard(v);
            v.clearFocus(); // Додатково знімаємо фокус з поля вводу
            return false;
        });

        sessionManager = new SessionManager(this);

        etUsername = findViewById(R.id.etRegUsername);
        etPassword = findViewById(R.id.etRegPassword);
        etRepeatPassword = findViewById(R.id.etRegRepeatPassword);
        btnRegister = findViewById(R.id.btnDoRegister);

        btnRegister.setOnClickListener(v -> performRegister());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Це стандартна функція Android "повернутися назад"
        return true;
    }

    private void performRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String repeatPassword = etRepeatPassword.getText().toString().trim();

        // 1. Валідація
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(repeatPassword)) {
            etRepeatPassword.setError("Passwords do not match");
            return;
        }

        if (!isValidPassword(password)) {
            etPassword.setError("Password must be >8 chars, contain A-Z, a-z and special char");
            return;
        }

        // 2. Запит до API
        LoginRequest request = new LoginRequest(username, password);
        RetrofitClient.getInstance(this).getApi().register(request)
                .enqueue(new Callback<TokenResponse>() {
                    @Override
                    public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Одразу логінимо юзера (зберігаємо токен)
                            sessionManager.saveToken(response.body().getAccessToken());

                            // Перехід в додаток
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<TokenResponse> call, Throwable t) {
                        Toast.makeText(RegisterActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Логіка валідації пароля
    private boolean isValidPassword(String password) {
        if (password.length() < 8) return false;

        Pattern upperCase = Pattern.compile("[A-Z]");
        Pattern lowerCase = Pattern.compile("[a-z]");
        Pattern special = Pattern.compile("[!@#$%^&*()_+=|<>?{}\\[\\]~-]");

        return upperCase.matcher(password).find() &&
                lowerCase.matcher(password).find() &&
                special.matcher(password).find();
    }

    private void hideKeyboard(android.view.View view) {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.app.Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}