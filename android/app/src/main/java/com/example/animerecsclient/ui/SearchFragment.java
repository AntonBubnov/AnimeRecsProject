package com.example.animerecsclient.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.animerecsclient.R;
import com.example.animerecsclient.viewmodel.SearchViewModel;
import com.example.animerecsclient.ui.bottomsheet.FilterBottomSheet;
import com.example.animerecsclient.ui.bottomsheet.SortingBottomSheet;
import com.example.animerecsclient.model.AppConstants;
import com.example.animerecsclient.model.FilterRequest;

public class SearchFragment extends Fragment {

    private SearchViewModel viewModel;
    private AnimeAdapter adapter;
    private EditText etSearch;
    private ProgressBar progressBar;

    // Для Debounce (затримки пошуку)
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.etSearchQuery);
        progressBar = view.findViewById(R.id.progressBarSearch);
        RecyclerView recyclerView = view.findViewById(R.id.rvSearchResults);

        // Налаштування списку (перевикористовуємо AnimeAdapter)
        adapter = new AnimeAdapter(getContext());
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Infinite Scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= 20) { // Не вантажити, якщо список занадто малий

                    viewModel.loadNextPage();
                }
            }
        });

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        // Підписка на результати
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), list -> {
            if (list != null) adapter.setAnimeList(list);
            else adapter.setAnimeList(new java.util.ArrayList<>()); // Очистка
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        // Обробка натискання на фон для приховування клавіатури
        view.findViewById(R.id.rvSearchResults).setOnTouchListener((v, event) -> {
            hideKeyboard(v);
            v.clearFocus(); // Знімаємо фокус з поля пошуку
            return false;
        });

        EditText etSearch = view.findViewById(R.id.etSearchQuery);

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString();
                viewModel.performSearch(query);

                // Ховаємо клавіатуру
                hideKeyboard(v);
                return true;
            }
            return false;
        });

        // Слухач вводу тексту з затримкою (Debounce)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Скасовуємо попередній запланований пошук
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Плануємо новий пошук через 500 мс
                searchRunnable = () -> viewModel.performSearch(s.toString());
                handler.postDelayed(searchRunnable, 500);
            }
        });
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Вмикаємо меню
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("Search Anime");
            ((MainActivity) getActivity()).showBackArrow(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_search, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sort) {
            showSortDialog(); // Ваш метод відкриття BottomSheet
            return true;
        } else if (id == R.id.action_filter) {
            showFilterDialog(); // Ваш метод відкриття BottomSheet
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Перенесіть логіку з setOnClickListener сюди
    private void showSortDialog() {
        SortingBottomSheet sheet = new SortingBottomSheet(
                viewModel.getCurrentFilter(),
                (sortBy, sortOrder) -> {
                    FilterRequest current = viewModel.getCurrentFilter();
                    current.sortBy = sortBy;
                    current.sortOrder = sortOrder;
                    viewModel.searchAdvanced(current, true);
                }
        );
        sheet.show(getParentFragmentManager(), "SortSheet");
    }
    private void showFilterDialog() {
        // Перевірка на null, щоб уникнути крашу
        if (viewModel.getConstants().getValue() == null) {
            // Можна показати Toast "Loading data..."
            return;
        }

        FilterBottomSheet sheet = new FilterBottomSheet(
                viewModel.getCurrentFilter(),
                viewModel.getConstants().getValue(),
                newFilter -> viewModel.searchAdvanced(newFilter, true)
        );
        sheet.show(getParentFragmentManager(), "FilterSheet");
    }
}