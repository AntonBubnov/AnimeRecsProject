package com.example.animerecsclient.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animerecsclient.R;
import com.example.animerecsclient.model.Anime;
import com.example.animerecsclient.viewmodel.RecommendationViewModel;

import java.util.ArrayList;
import java.util.List;

public class RecommendationFragment extends Fragment {

    private RecommendationViewModel viewModel;

    // UI Elements
    private View layoutDashboard, layoutGrid;
    private ProgressBar progressBar;

    // Adapters
    private HorizontalAnimeAdapter adapterPopular, adapterRank, adapterNew;
    private AnimeAdapter adapterGrid; // Використовуємо наш старий адаптер для сітки

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommendation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RecommendationViewModel.class);
        initUI(view);
        observeViewModel();
    }

    private void initUI(View view) {
        layoutDashboard = view.findViewById(R.id.layoutDashboard);
        layoutGrid = view.findViewById(R.id.layoutGrid);
        progressBar = view.findViewById(R.id.progressBarRecs);

        // --- 1. Налаштування Каруселей (Dashboard) ---
        setupCarousel(view.findViewById(R.id.rvPopular), "popularity", adapter -> adapterPopular = adapter);
        setupCarousel(view.findViewById(R.id.rvRank), "rank", adapter -> adapterRank = adapter);
        setupCarousel(view.findViewById(R.id.rvNew), "year", adapter -> adapterNew = adapter);

        // --- 2. Налаштування Сітки (Grid) ---
        RecyclerView rvGrid = view.findViewById(R.id.rvGrid);
        adapterGrid = new AnimeAdapter(getContext());
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        rvGrid.setLayoutManager(gridLayoutManager);
        rvGrid.setAdapter(adapterGrid);

        // Infinite Scroll для сітки
        rvGrid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (layoutGrid.getVisibility() != View.VISIBLE) return;

                int visibleItemCount = gridLayoutManager.getChildCount();
                int totalItemCount = gridLayoutManager.getItemCount();
                int firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                    viewModel.loadNextGridPage();
                }
            }
        });

        // Кнопка "Назад" у сітці
        Toolbar gridToolbar = view.findViewById(R.id.gridToolbar);
        gridToolbar.setNavigationOnClickListener(v -> closeGrid());
    }

    // Допоміжний метод для налаштування однієї каруселі
    private void setupCarousel(RecyclerView rv, String sortBy, AdapterAssigner assigner) {
        rv.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        HorizontalAnimeAdapter adapter = new HorizontalAnimeAdapter(getContext());
        rv.setAdapter(adapter);
        assigner.assign(adapter); // Зберігаємо посилання на адаптер
    }

    // Функціональний інтерфейс
    interface AdapterAssigner {
        void assign(HorizontalAnimeAdapter adapter);
    }

    private void updateCarouselData(HorizontalAnimeAdapter adapter, List<Anime> data, String sortBy, String title) {
        if (data == null) return;
        List<HorizontalAnimeAdapter.DisplayItem> items = new ArrayList<>();

        for (Anime anime : data) {
            items.add(new HorizontalAnimeAdapter.DisplayItem(
                    anime.getId(), anime.getDisplayTitle(), anime.getPictureMedium(),
                    String.format("%.1f", anime.getScore()) // Можна показувати score або рік
            ));
        }

        // Вмикаємо кнопку "Show More"
        adapter.setItems(items, true, () -> openGrid(sortBy, title));
    }

    private void observeViewModel() {
        viewModel.getPopularList().observe(getViewLifecycleOwner(), list ->
                updateCarouselData(adapterPopular, list, "popularity", "Most Popular"));

        viewModel.getRankList().observe(getViewLifecycleOwner(), list ->
                updateCarouselData(adapterRank, list, "rank", "Top Rated"));

        viewModel.getNewList().observe(getViewLifecycleOwner(), list ->
                updateCarouselData(adapterNew, list, "year", "New Releases"));

        // Дані для сітки
        viewModel.getGridList().observe(getViewLifecycleOwner(), list -> {
            if (list != null) adapterGrid.setAnimeList(list);
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));
    }

    private void openGrid(String sortBy, String title) {
        layoutDashboard.setVisibility(View.GONE);
        layoutGrid.setVisibility(View.VISIBLE);

        ((Toolbar) layoutGrid.findViewById(R.id.gridToolbar)).setTitle(title);
        viewModel.openGrid(sortBy);
    }

    private void closeGrid() {
        layoutGrid.setVisibility(View.GONE);
        layoutDashboard.setVisibility(View.VISIBLE);
        // Можна очистити gridList у ViewModel, щоб звільнити пам'ять
    }
}