package com.example.animerecsclient.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AnimeDetails {
    private int id;
    private String title;

    @SerializedName("main_picture")
    private Picture mainPicture;

    @SerializedName("alternative_titles")
    private AlternativeTitles alternativeTitles;

    private String synopsis;
    private double mean;
    private int rank;
    private int popularity;

    @SerializedName("user_status")
    private String userStatus; // "LIKE", "PLAN", etc.

    // Внутрішні класи для вкладених JSON об'єктів
    public static class Picture {
        public String medium;
        public String large;
    }

    public static class AlternativeTitles {
        public String en;
        public String ja;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getSynopsis() { return synopsis; }
    public String getUserStatus() { return userStatus; }
    public String getLargePicture() { return mainPicture != null ? mainPicture.large : null; }
    public String getEnglishTitle() { return (alternativeTitles != null && alternativeTitles.en != null) ? alternativeTitles.en : title; }
    public String getScoreString() { return "Score: " + mean + " (Rank #" + rank + ")"; }
}