package com.example.animerecsclient.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.animerecsclient.R;
import java.util.ArrayList;
import java.util.List;

public class HorizontalAnimeAdapter extends RecyclerView.Adapter<HorizontalAnimeAdapter.ViewHolder> {

    // Проста внутрішня модель для відображення
    public static class DisplayItem {
        int id;
        String title;
        String imageUrl;
        String subtitle; // "Sequel" або "15 Recs"

        public DisplayItem(int id, String title, String imageUrl, String subtitle) {
            this.id = id;
            this.title = title;
            this.imageUrl = imageUrl;
            this.subtitle = subtitle;
        }
    }

    private List<DisplayItem> items = new ArrayList<>();
    private Context context;

    public HorizontalAnimeAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<DisplayItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_anime_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DisplayItem item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvSubtitle.setText(item.subtitle);

        if (item.imageUrl != null) {
            Glide.with(context).load(item.imageUrl).centerCrop().into(holder.ivPoster);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AnimeDetailActivity.class);
            intent.putExtra("ANIME_ID", item.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle, tvSubtitle;
        public ViewHolder(View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }
}