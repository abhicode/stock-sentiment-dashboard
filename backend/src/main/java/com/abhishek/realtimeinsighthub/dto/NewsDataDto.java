package com.abhishek.realtimeinsighthub.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Value;

@Value
public class NewsDataDto {
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;
    private String stock;
    private Object newsData;

}
