package com.example.animerecsclient.model;

import com.google.gson.annotations.SerializedName;

public class Anime {
    private int id;
    private String title;
    private int popularity;

    // В Python ми назвали це 'score', 'profile_score' або просто передавали в JSON
    // Перевірте main_api.py, там response_model=AnimeResponse
    // AnimeResponse має поля: id, title, popularity, cluster_id, score

    private double score;

    @SerializedName("picture")
    private String pictureUrl; // URL картинки

    public int getId() { return id; }
    public String getTitle() { return title; }

    public int getPopularity() { return popularity; }
    public double getScore() { return score; }
    public String getPictureUrl() {
        return pictureUrl;
    }
}