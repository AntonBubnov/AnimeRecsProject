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

public class HorizontalAnimeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_MORE = 1;

    // Інтерфейс для кліку на "Show More"
    public interface OnShowMoreClickListener {
        void onShowMore();
    }

    public static class DisplayItem {
        int id;
        String title;
        String imageUrl;
        String subtitle;

        public DisplayItem(int id, String title, String imageUrl, String subtitle) {
            this.id = id;
            this.title = title;
            this.imageUrl = imageUrl;
            this.subtitle = subtitle;
        }
    }

    private List<DisplayItem> items = new ArrayList<>();
    private Context context;
    private boolean showMoreEnabled = false; // Чи показувати стрілку?
    private OnShowMoreClickListener moreListener;

    public HorizontalAnimeAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<DisplayItem> items, boolean enableShowMore, OnShowMoreClickListener listener) {
        this.items = items;
        this.showMoreEnabled = enableShowMore;
        this.moreListener = listener;
        notifyDataSetChanged();
    }

    // Перевантаження для сумісності зі старим кодом (де немає Show More)
    public void setItems(List<DisplayItem> items) {
        setItems(items, false, null);
    }

    @Override
    public int getItemViewType(int position) {
        if (showMoreEnabled && position == items.size()) {
            return TYPE_MORE;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MORE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_more, parent, false);
            return new MoreViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anime_horizontal, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            DisplayItem item = items.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            itemHolder.tvTitle.setText(item.title);
            itemHolder.tvSubtitle.setText(item.subtitle);
            if (item.imageUrl != null) {
                Glide.with(context).load(item.imageUrl).centerCrop().into(itemHolder.ivPoster);
            }

            itemHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, AnimeDetailActivity.class);
                intent.putExtra("ANIME_ID", item.id);
                context.startActivity(intent);
            });
        } else if (holder instanceof MoreViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                if (moreListener != null) moreListener.onShowMore();
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size() + (showMoreEnabled ? 1 : 0);
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPoster;
        TextView tvTitle, tvSubtitle;
        public ItemViewHolder(View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivPoster);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }
    }

    static class MoreViewHolder extends RecyclerView.ViewHolder {
        public MoreViewHolder(View itemView) {
            super(itemView);
        }
    }
}