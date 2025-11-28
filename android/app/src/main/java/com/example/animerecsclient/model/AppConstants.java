package com.example.animerecsclient.model;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AppConstants {
    public List<String> genres;
    @SerializedName("media_types")
    public List<String> mediaTypes;
}