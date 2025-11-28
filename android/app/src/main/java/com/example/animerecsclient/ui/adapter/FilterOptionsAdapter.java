package com.example.animerecsclient.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.animerecsclient.R;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilterOptionsAdapter extends RecyclerView.Adapter<FilterOptionsAdapter.ViewHolder> {

    public static final int STATE_NEUTRAL = 0;
    public static final int STATE_INCLUDE = 1; // Green
    public static final int STATE_EXCLUDE = 2; // Red

    private List<String> items;
    private Map<String, Integer> states = new HashMap<>();

    public FilterOptionsAdapter(List<String> items) {
        this.items = items;
    }

    // Встановити початкові стани (при відкритті вікна)
    public void setStates(List<String> included, List<String> excluded) {
        states.clear();
        for (String s : included) states.put(s, STATE_INCLUDE);
        for (String s : excluded) states.put(s, STATE_EXCLUDE);
        notifyDataSetChanged();
    }

    public Map<String, Integer> getStates() { return states; }

    public void reset() {
        states.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Використовуємо простий layout для тексту
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = items.get(position);
        int state = states.getOrDefault(item, STATE_NEUTRAL);

        holder.text.setText(item);
        holder.text.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        holder.text.setPadding(16, 16, 16, 16);

        // Візуалізація станів
        if (state == STATE_INCLUDE) {
            holder.itemView.setBackgroundColor(Color.parseColor("#C8E6C9")); // Green
        } else if (state == STATE_EXCLUDE) {
            holder.itemView.setBackgroundColor(Color.parseColor("#FFCDD2")); // Red
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT); // Default
        }

        // Логіка кліку (0 -> 1 -> 2 -> 0)
        holder.itemView.setOnClickListener(v -> {
            int newState = (state + 1) % 3;
            if (newState == STATE_NEUTRAL) states.remove(item);
            else states.put(item, newState);
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        public ViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(android.R.id.text1);
        }
    }
}