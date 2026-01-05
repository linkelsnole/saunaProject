package com.example.modbus_api.modbus.model;

import retrofit2.Response;

public class Utils {
    public static String httpError(Response<?> response) {
        try {
            return response.errorBody() != null ? response.errorBody().string() : "Unknown error";
        } catch (Exception e) {
            return "Error reading response: " + e.getMessage();
        }
    }
}
