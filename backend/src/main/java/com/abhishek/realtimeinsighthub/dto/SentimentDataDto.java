package com.abhishek.realtimeinsighthub.dto;

import java.time.Instant;

import lombok.Value;

@Value
public class SentimentDataDto {

    private Instant timestamp;
    private String stock;
    private String sentiment;
    private double compound;
}
