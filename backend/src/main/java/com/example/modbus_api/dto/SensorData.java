package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {
	private String id;
	private String name;
	private String value;
	private String unit;
}
