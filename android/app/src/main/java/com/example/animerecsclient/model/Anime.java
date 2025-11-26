package com.example.animerecsclient.model;
import com.google.gson.annotations.SerializedName;

public class Anime {
    private int id;
    private String title;

    @SerializedName("title_en")
    private String titleEn;     // Нове поле

    @SerializedName("picture_medium")
    private String pictureMedium; // Нове поле

    @SerializedName("picture_large")
    private String pictureLarge;  // Нове поле

    private int popularity;
    private double score;

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }

    // Логіка: Якщо англійська назва є, повертаємо її. Якщо ні - оригінальну.
    public String getDisplayTitle() {
        return (titleEn != null && !titleEn.isEmpty()) ? titleEn : title;
    }

    public String getPictureMedium() { return pictureMedium; }
    public String getPictureLarge() { return pictureLarge; }
    public int getPopularity() { return popularity; }
    public double getScore() { return score; }
}