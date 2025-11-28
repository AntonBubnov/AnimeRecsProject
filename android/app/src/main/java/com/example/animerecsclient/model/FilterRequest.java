package com.example.animerecsclient.model;
import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class FilterRequest {
    @SerializedName("sort_by") public String sortBy = "popularity";
    @SerializedName("sort_order") public String sortOrder = "desc";

    @SerializedName("genres_include") public List<String> genresInclude = new ArrayList<>();
    @SerializedName("genres_exclude") public List<String> genresExclude = new ArrayList<>();
    @SerializedName("genres_strict") public boolean genresStrict = false;

    @SerializedName("media_types_include") public List<String> mediaTypesInclude = new ArrayList<>();
    @SerializedName("media_types_exclude") public List<String> mediaTypesExclude = new ArrayList<>();

    @SerializedName("year_from") public Integer yearFrom;
    @SerializedName("year_to") public Integer yearTo;
    @SerializedName("score_from") public Double scoreFrom;
    @SerializedName("score_to") public Double scoreTo;

    public int page = 1;
    public int limit = 50;

    // Метод для клонування (щоб не псувати поточний стан при редагуванні)
    public FilterRequest copy() {
        FilterRequest copy = new FilterRequest();
        copy.sortBy = this.sortBy;
        copy.sortOrder = this.sortOrder;
        copy.genresInclude = new ArrayList<>(this.genresInclude);
        copy.genresExclude = new ArrayList<>(this.genresExclude);
        copy.genresStrict = this.genresStrict;
        copy.mediaTypesInclude = new ArrayList<>(this.mediaTypesInclude);
        copy.mediaTypesExclude = new ArrayList<>(this.mediaTypesExclude);
        copy.yearFrom = this.yearFrom;
        copy.yearTo = this.yearTo;
        copy.scoreFrom = this.scoreFrom;
        copy.scoreTo = this.scoreTo;
        copy.page = this.page;
        copy.limit = this.limit;
        return copy;
    }
}