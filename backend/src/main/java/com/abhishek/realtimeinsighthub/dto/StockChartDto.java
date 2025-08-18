package com.abhishek.realtimeinsighthub.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Value;

@Value
public class StockChartDto {
    private Instant timestamp;
    private BigDecimal price;
    private String sentiment;
    private Double compound;
}
