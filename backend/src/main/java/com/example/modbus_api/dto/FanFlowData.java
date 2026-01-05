package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FanFlowData {
	private String type;           // Приток/Вытяжка
    private Integer speedPerc;     // (пока что нет)
    private Integer vm;     // (пока что нет)
    private Double pressure;       // Текущий объем (BF11/BF21)
	private Double targetAirFlow;        // Расход м3/ч 
}
