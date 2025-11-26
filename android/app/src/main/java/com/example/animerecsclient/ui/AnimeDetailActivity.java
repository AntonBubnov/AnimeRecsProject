package com.example.animerecsclient.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.animerecsclient.R;
import com.example.animerecsclient.model.AnimeDetails;
import com.example.animerecsclient.viewmodel.AnimeDetailViewModel;

public class AnimeDetailActivity extends AppCompatActivity {
    private AnimeDetailViewModel viewModel;
    private int animeId;

    private TextView tvMainTitle, tvEnglishTitle, tvSynopsis, tvShowMore, tvUserStatus;
    private TextView tvMediaType, tvSeason, tvRank, tvMeanScore, tvGenres;
    private ImageView ivPoster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime_detail);

        // --- UI Setup ---
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        // Отримуємо ID з Intent
        animeId = getIntent().getIntExtra("ANIME_ID", -1);
        if (animeId == -1) finish();

        // Ініціалізація View-елементів (тут багато findViewById)
        initializeViews();

        viewModel = new ViewModelProvider(this).get(AnimeDetailViewModel.class);
        viewModel.loadDetails(animeId);

        // --- Підписка на дані ---
        viewModel.getAnimeDetails().observe(this, this::updateUI);

        // --- Кнопки ---
        setupButtons();
    }

    // Обробка стрілки "Назад"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Внутрішні методи для організації коду
    private void initializeViews() {
        // Замініть ці findViewById на ваші реальні ID з layout
        tvMainTitle = findViewById(R.id.tvMainTitle);
        tvEnglishTitle = findViewById(R.id.tvEnglishTitle);
        tvSynopsis = findViewById(R.id.tvSynopsis);
        tvShowMore = findViewById(R.id.tvShowMore);
        ivPoster = findViewById(R.id.ivDetailPoster);
        tvUserStatus = findViewById(R.id.tvUserStatus);

        // Знаходимо елементи з detail_info_row (якщо підключено через <include>)
        tvMediaType = findViewById(R.id.tvMediaType);
        tvSeason = findViewById(R.id.tvSeason);
        tvRank = findViewById(R.id.tvRank);
        tvMeanScore = findViewById(R.id.tvMeanScore);
        tvGenres = findViewById(R.id.tvGenres);
    }

    private void setupButtons() {
        findViewById(R.id.btnDetailDislike).setOnClickListener(v -> viewModel.updateStatus(animeId, "DISLIKE"));
        findViewById(R.id.btnDetailPlan).setOnClickListener(v -> viewModel.updateStatus(animeId, "PLAN"));
        findViewById(R.id.btnDetailLike).setOnClickListener(v -> viewModel.updateStatus(animeId, "LIKE"));

        // Логіка розкриття синопсису
        tvShowMore.setOnClickListener(v -> {
            if (tvSynopsis.getMaxLines() == 4) {
                tvSynopsis.setMaxLines(Integer.MAX_VALUE);
                tvShowMore.setText("Show Less");
            } else {
                tvSynopsis.setMaxLines(4);
                tvShowMore.setText("Show More");
            }
        });
    }

    private void updateUI(AnimeDetails details) {
        if (details == null) return;

        // Заголовки
        tvMainTitle.setText(details.getTitle());
        tvEnglishTitle.setText(details.getEnglishTitle());

        // Інфо-рядок
        tvMediaType.setText(details.getMediaType());
        tvSeason.setText(details.getSeason());
        tvRank.setText(details.getRank());
        tvMeanScore.setText(details.getScore());

        // Синопсис
        tvSynopsis.setText(details.getSynopsis());

        // Жанри
        if (details.getGenres() != null) {
            StringBuilder genreText = new StringBuilder("Genres: ");
            for (AnimeDetails.Genre genre : details.getGenres()) {
                genreText.append(genre.name).append(", ");
            }
            tvGenres.setText(genreText.substring(0, genreText.length() - 2)); // Видаляємо останню кому
        }

        // Статус користувача
        String status = details.getUserStatus() != null ? details.getUserStatus() : "None";
        tvUserStatus.setText("Status: " + status);

        // Картинка
        Glide.with(this).load(details.getLargePicture()).into(ivPoster);
    }
}