package com.example.animerecsclient.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.animerecsclient.R;
import com.example.animerecsclient.viewmodel.LibraryViewModel;

public class LibraryListFragment extends Fragment {

    private static final String ARG_STATUS = "status";
    private String status;
    private LibraryViewModel viewModel;
    private AnimeAdapter adapter;

    // Метод для створення фрагмента з параметром
    public static LibraryListFragment newInstance(String status) {
        LibraryListFragment fragment = new LibraryListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            status = getArguments().getString(ARG_STATUS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Використовуємо той самий макет, що і для рекомендацій (там просто RecyclerView)
        return inflater.inflate(R.layout.fragment_recommendation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.rvAnimeFeed);
        // Приховуємо прогрес бар, бо логіка простіша
        view.findViewById(R.id.progressBar).setVisibility(View.GONE);

        adapter = new AnimeAdapter(getContext());
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); // Або 3 для компактності
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);
        viewModel.getLibraryList().observe(getViewLifecycleOwner(), animes -> {
            adapter.setAnimeList(animes);
        });

        // Завантажуємо дані для конкретного статусу
        viewModel.loadLibrary(status);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Оновлюємо список при поверненні (на випадок, якщо юзер змінив статус в деталях)
        viewModel.loadLibrary(status);
    }
}