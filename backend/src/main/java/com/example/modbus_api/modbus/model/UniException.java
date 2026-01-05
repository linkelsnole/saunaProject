package com.example.modbus_api.modbus.model;

import java.io.IOException;

public class UniException extends IOException {
    public UniException(String message) {
        super(message);
    }

    public static UniException io(String message) {
        return new UniException(message);
    }

    public static UniException io(Exception ex) {
        return new UniException(ex.getMessage());
    }
}
