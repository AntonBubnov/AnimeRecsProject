package com.example.animerecsclient.ui.bottomsheet;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.animerecsclient.R;
import com.example.animerecsclient.model.AppConstants;
import com.example.animerecsclient.model.FilterRequest;
import com.example.animerecsclient.ui.adapter.FilterOptionsAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    private FilterRequest filter;
    private AppConstants constants;
    private OnFilterAppliedListener listener;

    private FilterOptionsAdapter genreAdapter;
    private FilterOptionsAdapter mediaAdapter;

    private CheckBox cbStrict;
    private EditText etYearFrom, etYearTo, etScoreFrom, etScoreTo;

    public interface OnFilterAppliedListener {
        void onFilterApplied(FilterRequest newFilter);
    }

    public FilterBottomSheet(FilterRequest filter, AppConstants constants, OnFilterAppliedListener listener) {
        this.filter = filter.copy(); // Працюємо з копією, щоб не ламати оригінал до натискання Apply
        this.constants = constants;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Ініціалізація UI
        RecyclerView rvGenres = view.findViewById(R.id.rvGenres);
        RecyclerView rvMedia = view.findViewById(R.id.rvMediaTypes);
        cbStrict = view.findViewById(R.id.cbStrictGenres);
        etYearFrom = view.findViewById(R.id.etYearFrom);
        etYearTo = view.findViewById(R.id.etYearTo);
        etScoreFrom = view.findViewById(R.id.etScoreFrom);
        etScoreTo = view.findViewById(R.id.etScoreTo);

        // Налаштування адаптерів
        if (constants != null) {
            // Жанри (2 колонки)
            genreAdapter = new FilterOptionsAdapter(constants.genres != null ? constants.genres : new ArrayList<>());
            genreAdapter.setStates(filter.genresInclude, filter.genresExclude);
            rvGenres.setLayoutManager(new GridLayoutManager(getContext(), 2));
            rvGenres.setAdapter(genreAdapter);

            // Медіа (2 колонки)
            mediaAdapter = new FilterOptionsAdapter(constants.mediaTypes != null ? constants.mediaTypes : new ArrayList<>());
            mediaAdapter.setStates(filter.mediaTypesInclude, filter.mediaTypesExclude);
            rvMedia.setLayoutManager(new GridLayoutManager(getContext(), 2));
            rvMedia.setAdapter(mediaAdapter);
        }

        // Відновлення значень полів
        cbStrict.setChecked(filter.genresStrict);
        if (filter.yearFrom != null) etYearFrom.setText(String.valueOf(filter.yearFrom));
        if (filter.yearTo != null) etYearTo.setText(String.valueOf(filter.yearTo));
        if (filter.scoreFrom != null) etScoreFrom.setText(String.valueOf(filter.scoreFrom));
        if (filter.scoreTo != null) etScoreTo.setText(String.valueOf(filter.scoreTo));

        // Кнопки
        view.findViewById(R.id.btnCloseFilter).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnReset).setOnClickListener(v -> resetFilters());
        view.findViewById(R.id.btnApply).setOnClickListener(v -> applyFilters());
    }

    private void resetFilters() {
        genreAdapter.reset();
        mediaAdapter.reset();
        cbStrict.setChecked(false);
        etYearFrom.setText(""); etYearTo.setText("");
        etScoreFrom.setText(""); etScoreTo.setText("");
    }

    private void applyFilters() {
        // Збираємо дані з адаптерів
        filter.genresInclude.clear();
        filter.genresExclude.clear();
        for (Map.Entry<String, Integer> entry : genreAdapter.getStates().entrySet()) {
            if (entry.getValue() == FilterOptionsAdapter.STATE_INCLUDE) filter.genresInclude.add(entry.getKey());
            if (entry.getValue() == FilterOptionsAdapter.STATE_EXCLUDE) filter.genresExclude.add(entry.getKey());
        }

        filter.mediaTypesInclude.clear();
        filter.mediaTypesExclude.clear();
        for (Map.Entry<String, Integer> entry : mediaAdapter.getStates().entrySet()) {
            if (entry.getValue() == FilterOptionsAdapter.STATE_INCLUDE) filter.mediaTypesInclude.add(entry.getKey());
            if (entry.getValue() == FilterOptionsAdapter.STATE_EXCLUDE) filter.mediaTypesExclude.add(entry.getKey());
        }

        filter.genresStrict = cbStrict.isChecked();
        filter.yearFrom = parseInt(etYearFrom);
        filter.yearTo = parseInt(etYearTo);
        filter.scoreFrom = parseDouble(etScoreFrom);
        filter.scoreTo = parseDouble(etScoreTo);

        if (listener != null) listener.onFilterApplied(filter);
        dismiss();
    }

    private Integer parseInt(EditText et) {
        String s = et.getText().toString();
        return TextUtils.isEmpty(s) ? null : Integer.parseInt(s);
    }

    private Double parseDouble(EditText et) {
        String s = et.getText().toString();
        return TextUtils.isEmpty(s) ? null : Double.parseDouble(s);
    }
}