package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClimateSensorDto {
    private String id; 
    private Double temperature; 
    private Double humidity;   
    private String label;     
}