package com.abhishek.realtimeinsighthub.dto;

import java.math.BigDecimal;
import java.time.Instant;

import lombok.Value;

@Value
public class StockDataDto {
    private Instant timestamp;
    private String stock;
    private BigDecimal price;
}
