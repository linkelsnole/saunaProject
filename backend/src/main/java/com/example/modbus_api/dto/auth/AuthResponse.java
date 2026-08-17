package com.example.modbus_api.dto.auth;

public class AuthResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresInMs;
    private final UserInfoResponse user;

    public AuthResponse(String accessToken, String tokenType, long expiresInMs, UserInfoResponse user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public UserInfoResponse getUser() {
        return user;
    }
}
