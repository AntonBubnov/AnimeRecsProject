package com.example.animerecsclient.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.animerecsclient.R;
import com.example.animerecsclient.viewmodel.AnimeDetailViewModel;

public class AnimeDetailActivity extends AppCompatActivity {
    private AnimeDetailViewModel viewModel;
    private int animeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anime_detail);

        // Отримуємо ID з Intent
        animeId = getIntent().getIntExtra("ANIME_ID", -1);
        if (animeId == -1) finish();

        viewModel = new ViewModelProvider(this).get(AnimeDetailViewModel.class);
        viewModel.loadDetails(animeId);

        ImageView ivPoster = findViewById(R.id.ivDetailPoster);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvSynopsis = findViewById(R.id.tvSynopsis);
        TextView tvStatus = findViewById(R.id.tvUserStatus);

        // Підписка
        viewModel.getAnimeDetails().observe(this, details -> {
            tvTitle.setText(details.getEnglishTitle());
            tvSynopsis.setText(details.getSynopsis());
            tvStatus.setText("Status: " + (details.getUserStatus() != null ? details.getUserStatus() : "None"));
            Glide.with(this).load(details.getLargePicture()).into(ivPoster);
        });

        // Кнопки
        findViewById(R.id.btnDetailLike).setOnClickListener(v -> viewModel.updateStatus(animeId, "LIKE"));
        findViewById(R.id.btnDetailPlan).setOnClickListener(v -> viewModel.updateStatus(animeId, "PLAN"));
        findViewById(R.id.btnDetailDislike).setOnClickListener(v -> viewModel.updateStatus(animeId, "DISLIKE"));
    }
}