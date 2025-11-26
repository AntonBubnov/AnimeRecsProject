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

        // 4. Налаштування Infinite Scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) rv.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    // Якщо ми прокрутили до кінця списку і не вантажимося прямо зараз
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= 50) { // Перевірка, що список не порожній

                        viewModel.loadNextPage();
                    }
                }
            }
        });

        // 5. Підписка на дані (залишається майже без змін)
        viewModel.getAnimeList().observe(getViewLifecycleOwner(), animeList -> {
            progressBar.setVisibility(View.GONE);
            if (animeList != null) { // Прибрав перевірку isEmpty(), щоб не ховати список при оновленні
                adapter.setAnimeList(animeList);
                tvError.setVisibility(View.GONE);
            }
        });

        // 6. Підписка на помилки
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            progressBar.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(error);
            Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
        });
    }
}