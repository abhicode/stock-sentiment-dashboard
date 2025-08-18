package com.abhishek.realtimeinsighthub.dto;

import java.time.Instant;
import java.util.Map;

import lombok.Value;

@Value
public class SentimentResponseDto {

    private Instant timestamp;
    private String stock;
    private String sentiment;
    private Map<String, Double> scores;
}
