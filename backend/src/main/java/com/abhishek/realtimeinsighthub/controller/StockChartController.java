package com.abhishek.realtimeinsighthub.controller;

import java.time.Duration;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.abhishek.realtimeinsighthub.dto.StockChartDto;
import com.abhishek.realtimeinsighthub.service.StockChartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chart")
@RequiredArgsConstructor
public class StockChartController {

    private final StockChartService stockChartService;

    @GetMapping("/{stock}/live")
    public List<StockChartDto> getLiveChart(@PathVariable String stock) {
        // pre-fill the data and websocket keep it live
        return stockChartService.getChartData(stock, Duration.ofHours(1));
    }

    @GetMapping("/{stock}/trend")
    public List<StockChartDto> getWeeklyChart(@PathVariable String stock,
        @RequestParam(defaultValue = "7d") String range) {
        Duration period = parseRange(range);
        return stockChartService.getChartData(stock, period);
    }

    private Duration parseRange(String range) {
        switch (range) {
            case "1d": return Duration.ofDays(1);
            case "7d": return Duration.ofDays(7);
            case "1m": return Duration.ofDays(30);
            default: throw new IllegalArgumentException("Unsupported range: " + range);
        }
    }

}
