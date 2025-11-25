package com.example.animerecsclient.model;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    // @SerializedName вказує, як це поле називається в JSON.
    // Це важливо, якщо в Java ми хочемо назвати поле інакше (camelCase),
    // а в Python воно snake_case.

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("token_type")
    private String tokenType;

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
}