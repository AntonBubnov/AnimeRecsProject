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
    @SerializedName("media_type")
    private String mediaType;
    @SerializedName("start_season")
    private StartSeason startSeason;
    private List<Genre> genres; // Список жанрів

    @SerializedName("user_status")
    private String userStatus; // "LIKE", "PLAN", etc.

    @SerializedName("related_anime")
    private List<RelatedEntry> relatedAnime;

    @SerializedName("recommendations")
    private List<RecommendationEntry> recommendations;

    // Внутрішні класи для вкладених JSON об'єктів
    public static class Picture {
        public String medium;
        public String large;
    }

    public static class AlternativeTitles {
        public String en;
        public String ja;
    }

    // Внутрішній клас для сезону
    public static class StartSeason {
        public String season;
        public int year;
        public String getDisplay() { return season + " " + year; }
    }

    // Внутрішній клас для жанрів (або просто String)
    public static class Genre {
        public String name;
    }

    public static class RelatedEntry {
        @SerializedName("node")
        public Node node;
        @SerializedName("relation_type_formatted")
        public String relationType;
    }

    public static class RecommendationEntry {
        @SerializedName("node")
        public Node node;
        @SerializedName("num_recommendations")
        public int numRecommendations;
    }

    // Загальний клас вузла (аніме в списку)
    public static class Node {
        public int id;
        public String title;
        @SerializedName("main_picture")
        public Picture mainPicture;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getSynopsis() { return synopsis; }
    public String getUserStatus() { return userStatus; }
    public String getLargePicture() { return mainPicture != null ? mainPicture.large : null; }
    public String getMediumPicture() { return mainPicture != null ? mainPicture.medium : null; }
    public String getEnglishTitle() { return (alternativeTitles != null && alternativeTitles.en != null) ? alternativeTitles.en : title; }
    public String getMediaType() { return mediaType; }
    public String getSeason() { return startSeason != null ? startSeason.getDisplay() : "N/A"; }
    public String getRank() { return "#" + rank; }
    public String getScore() { return String.valueOf(mean); }
    public List<Genre> getGenres() { return genres; }

    public List<RelatedEntry> getRelatedAnime() { return relatedAnime; }
    public List<RecommendationEntry> getRecommendations() { return recommendations; }
}