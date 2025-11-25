package com.example.animerecsclient.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.animerecsclient.R;
import com.example.animerecsclient.model.Anime;
import com.example.animerecsclient.viewmodel.EvaluationViewModel;

public class EvaluationFragment extends Fragment {

    private EvaluationViewModel viewModel;

    private ImageView ivPoster;
    private TextView tvTitle, tvPopularity;
    private ImageButton btnLike, btnDislike, btnSkip, btnPlan;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_evaluation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ініціалізація UI
        ivPoster = view.findViewById(R.id.ivAnimePoster);
        tvTitle = view.findViewById(R.id.tvAnimeTitle);
        tvPopularity = view.findViewById(R.id.tvAnimePopularity);

        btnLike = view.findViewById(R.id.btnLike);
        btnDislike = view.findViewById(R.id.btnDislike);
        btnSkip = view.findViewById(R.id.btnSkip);
        btnPlan = view.findViewById(R.id.btnPlan);

        // 2. Ініціалізація ViewModel
        viewModel = new ViewModelProvider(this).get(EvaluationViewModel.class);

        // 3. Підписка на оновлення даних (Observer)
        viewModel.getCurrentAnime().observe(getViewLifecycleOwner(), anime -> {
            updateUI(anime);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });

        // 4. Обробка натискань
        setupButtons();
    }

    private void updateUI(Anime anime) {
        tvTitle.setText(anime.getTitle());
        tvPopularity.setText("Popularity: " + anime.getPopularity());

        // Завантаження картинки через Glide
        if (anime.getPictureUrl() != null && !anime.getPictureUrl().isEmpty()) {
            Glide.with(this)
                    .load(anime.getPictureUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery) // Картинка поки вантажиться
                    .error(android.R.drawable.stat_notify_error)     // Якщо помилка
                    .into(ivPoster);
        }
    }

    private void setupButtons() {
        btnLike.setOnClickListener(v -> viewModel.rateCurrentAnime("LIKE"));
        btnDislike.setOnClickListener(v -> viewModel.rateCurrentAnime("DISLIKE"));
        btnSkip.setOnClickListener(v -> viewModel.rateCurrentAnime("SKIP"));
        btnPlan.setOnClickListener(v -> viewModel.rateCurrentAnime("PLAN"));
    }
}