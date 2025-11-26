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
    private TextView tvTitle, tvEnTitle;
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
        tvEnTitle = view.findViewById(R.id.tvAnimeEnTitle);

        btnLike = view.findViewById(R.id.btnLike);
        btnDislike = view.findViewById(R.id.btnDislike);
        btnSkip = view.findViewById(R.id.btnSkip);
        btnPlan = view.findViewById(R.id.btnPlan);

        // Отримуємо кореневий View, щоб він реагував на натискання
        View rootView = view.findViewById(R.id.fragment_root_layout);
        if (rootView != null) {
            rootView.setOnClickListener(v -> {
                Anime currentAnime = viewModel.getCurrentAnime().getValue();
                if (currentAnime != null) {
                    android.content.Intent intent = new android.content.Intent(getContext(), AnimeDetailActivity.class);
                    intent.putExtra("ANIME_ID", currentAnime.getId());
                    startActivity(intent);
                }
            });
        }

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

        // Використовуємо розумний гетер для назви
        tvEnTitle.setText(anime.getDisplayTitle());

        // Використовуємо ВЕЛИКУ картинку
        if (anime.getPictureLarge() != null && !anime.getPictureLarge().isEmpty()) {
            Glide.with(this)
                    .load(anime.getPictureLarge())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .fitCenter() // Для великої картинки краще fitCenter
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