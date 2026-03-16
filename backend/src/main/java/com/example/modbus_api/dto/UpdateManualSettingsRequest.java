package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateManualSettingsRequest {
    private Double supplySpeed;  // В процентах (90.1%)
    private Double boostPercent; // В процентах (15.4%)
}

