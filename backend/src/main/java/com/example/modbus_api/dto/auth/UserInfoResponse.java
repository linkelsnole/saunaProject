package com.example.modbus_api.dto.auth;

import com.example.modbus_api.security.model.AppUser;

public class UserInfoResponse {

    private final String id;
    private final String username;
    private final String role;

    public UserInfoResponse(String id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public static UserInfoResponse from(AppUser user) {
        return new UserInfoResponse(String.valueOf(user.getId()), user.getUsername(), user.getRole().name());
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
