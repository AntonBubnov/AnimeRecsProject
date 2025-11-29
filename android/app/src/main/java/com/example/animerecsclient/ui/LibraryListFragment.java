package com.example.animerecsclient.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
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

    // UI Elements
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvError;

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
        // ВИПРАВЛЕННЯ: Використовуємо власний макет, а не макет рекомендацій
        return inflater.inflate(R.layout.fragment_library_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ініціалізація View (з новими ID)
        recyclerView = view.findViewById(R.id.rvLibraryList);
        progressBar = view.findViewById(R.id.pbLibrary);
        tvError = view.findViewById(R.id.tvLibraryError);

        // 2. Налаштування списку
        adapter = new AnimeAdapter(getContext());
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setAdapter(adapter);

        // 3. ViewModel
        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        // 4. Підписка
        viewModel.getLibraryList().observe(getViewLifecycleOwner(), animes -> {
            progressBar.setVisibility(View.GONE);
            if (animes != null && !animes.isEmpty()) {
                adapter.setAnimeList(animes);
                tvError.setVisibility(View.GONE);
            } else {
                // Показуємо повідомлення, якщо список порожній
                adapter.setAnimeList(new java.util.ArrayList<>());
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("No items yet");
            }
        });

        // Завантаження
        viewModel.loadLibrary(status);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadLibrary(status);
    }
}