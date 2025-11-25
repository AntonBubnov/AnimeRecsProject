package com.example.animerecsclient.model;

import com.google.gson.annotations.SerializedName;

public class RateRequest {

    // Використовуємо SerializedName, щоб Java-стиль (camelCase)
    // перетворився на Python-стиль (snake_case) у JSON.
    @SerializedName("anime_id")
    private int animeId;

    @SerializedName("rating_type")
    private String ratingType; // "LIKE", "DISLIKE", "SKIP", "PLAN"

    public RateRequest(int animeId, String ratingType) {
        this.animeId = animeId;
        this.ratingType = ratingType;
    }

    public int getAnimeId() {
        return animeId;
    }

    public void setAnimeId(int animeId) {
        this.animeId = animeId;
    }

    public String getRatingType() {
        return ratingType;
    }

    public void setRatingType(String ratingType) {
        this.ratingType = ratingType;
    }
}