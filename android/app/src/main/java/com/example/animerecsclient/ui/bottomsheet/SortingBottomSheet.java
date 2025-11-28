package com.example.animerecsclient.ui.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.animerecsclient.R;
import com.example.animerecsclient.model.FilterRequest;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SortingBottomSheet extends BottomSheetDialogFragment {

    private FilterRequest currentFilter;
    private OnSortSelectedListener listener;

    public interface OnSortSelectedListener {
        void onSortApplied(String sortBy, String sortOrder);
    }

    public SortingBottomSheet(FilterRequest filter, OnSortSelectedListener listener) {
        this.currentFilter = filter;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_sort, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RadioGroup rgBy = view.findViewById(R.id.rgSortBy);
        RadioGroup rgOrder = view.findViewById(R.id.rgSortOrder);

        // 1. Відновлюємо стан
        setRadioState(rgBy, currentFilter.sortBy);
        setOrderState(rgOrder, currentFilter.sortOrder);

        // 2. Слухачі (Авто-застосування)
        rgBy.setOnCheckedChangeListener((group, checkedId) -> applySort(rgBy, rgOrder));
        rgOrder.setOnCheckedChangeListener((group, checkedId) -> applySort(rgBy, rgOrder));

        view.findViewById(R.id.btnCloseSort).setOnClickListener(v -> dismiss());
    }

    private void applySort(RadioGroup rgBy, RadioGroup rgOrder) {
        String by = "popularity";
        int byId = rgBy.getCheckedRadioButtonId();
        if (byId == R.id.rbRank) by = "rank";
        else if (byId == R.id.rbYear) by = "start_season";
        else if (byId == R.id.rbTitle) by = "title";

        String order = (rgOrder.getCheckedRadioButtonId() == R.id.rbAsc) ? "asc" : "desc";

        if (listener != null) listener.onSortApplied(by, order);
    }

    private void setRadioState(RadioGroup rg, String val) {
        if (val.equals("rank")) rg.check(R.id.rbRank);
        else if (val.equals("start_season")) rg.check(R.id.rbYear);
        else if (val.equals("title")) rg.check(R.id.rbTitle);
        else rg.check(R.id.rbPopularity);
    }

    private void setOrderState(RadioGroup rg, String val) {
        rg.check(val.equals("asc") ? R.id.rbAsc : R.id.rbDesc);
    }
}