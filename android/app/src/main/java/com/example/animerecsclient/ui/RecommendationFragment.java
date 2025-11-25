package com.example.animerecsclient.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animerecsclient.R;
import com.example.animerecsclient.viewmodel.RecommendationViewModel;

public class RecommendationFragment extends Fragment {

    private RecommendationViewModel viewModel;
    private AnimeAdapter adapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommendation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ініціалізація View
        recyclerView = view.findViewById(R.id.rvAnimeFeed);
        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);

        // 2. Налаштування RecyclerView (Сітка з 2 колонок)
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Створюємо та підключаємо адаптер
        adapter = new AnimeAdapter(getContext());
        recyclerView.setAdapter(adapter);

        // 3. Ініціалізація ViewModel
        viewModel = new ViewModelProvider(this).get(RecommendationViewModel.class);

        // 4. Підписка на дані
        viewModel.getAnimeList().observe(getViewLifecycleOwner(), animeList -> {
            // Дані прийшли!
            progressBar.setVisibility(View.GONE); // Ховаємо спінер
            if (animeList != null && !animeList.isEmpty()) {
                adapter.setAnimeList(animeList); // Оновлюємо список
                tvError.setVisibility(View.GONE);
            } else {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("No recommendations yet.");
            }
        });

        // 5. Підписка на помилки
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            progressBar.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(error);
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }
}