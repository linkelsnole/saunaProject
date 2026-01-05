package com.example.modbus_api.security.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}