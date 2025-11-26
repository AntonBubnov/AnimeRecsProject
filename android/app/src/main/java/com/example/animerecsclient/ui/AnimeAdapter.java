package com.example.animerecsclient.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.animerecsclient.R;
import com.example.animerecsclient.model.Anime;

import java.util.ArrayList;
import java.util.List;

public class AnimeAdapter extends RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder> {

    private List<Anime> animeList = new ArrayList<>(); // Наші "книги"
    private Context context;

    public AnimeAdapter(Context context) {
        this.context = context;
    }

    // Метод для оновлення даних у списку
    public void setAnimeList(List<Anime> animeList) {
        this.animeList = animeList;
        notifyDataSetChanged(); // Кажемо "полиці", що треба перемалювати все
    }

    // 1. Створення нової "комірки" (ViewHolder)
    // Викликається тільки коли треба створити нову картку фізично
    @NonNull
    @Override
    public AnimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_anime, parent, false);
        return new AnimeViewHolder(view);
    }

    // 2. Заповнення даними (Binding)
    // Викликається постійно, коли ми гортаємо список
    @Override
    public void onBindViewHolder(@NonNull AnimeViewHolder holder, int position) {
        Anime anime = animeList.get(position);

        holder.tvTitle.setText(anime.getTitle());

        holder.tvEnTitle.setText(anime.getDisplayTitle());

        // Використовуємо СЕРЕДНЮ картинку (для швидкості списку)
        if (anime.getPictureMedium() != null && !anime.getPictureMedium().isEmpty()) {
            Glide.with(context)
                    .load(anime.getPictureMedium())
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivPoster);
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, AnimeDetailActivity.class);
            intent.putExtra("ANIME_ID", anime.getId());
            context.startActivity(intent);
        });
    }

    // 3. Скільки всього елементів?
    @Override
    public int getItemCount() {
        return animeList.size();
    }

    // Внутрішній клас, який тримає посилання на елементи View (щоб не шукати їх щоразу)
    static class AnimeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle, tvEnTitle;

        public AnimeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvEnTitle = itemView.findViewById(R.id.tvEnTitle);
        }
    }
}