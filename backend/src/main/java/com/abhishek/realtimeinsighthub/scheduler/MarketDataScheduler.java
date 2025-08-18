package com.abhishek.realtimeinsighthub.scheduler;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.abhishek.realtimeinsighthub.service.publisher.MarketDataService;
import com.abhishek.realtimeinsighthub.service.publisher.NewsDataService;
import com.abhishek.realtimeinsighthub.service.publisher.StockDataService;

@Component
public class MarketDataScheduler {
    private final List<MarketDataService> services;

    public MarketDataScheduler(List<MarketDataService> services) {
        this.services = services;
    }

    // triggers every 10 minutes
    @Scheduled(fixedRate = 600000)
    public void fetchStockDataService() {
        if (isUSMarketOpen()) {
            services.stream()
                .filter(s -> s instanceof StockDataService)
                .forEach(MarketDataService::fetchAndPublishData);
        }
    }

    // triggers every 30 minutes
    @Scheduled(fixedRate = 1800000)
    public void fetchNewsDataService() {
        services.stream()
            .filter(s -> s instanceof NewsDataService)
            .forEach(MarketDataService::fetchAndPublishData);
    }

    public static boolean isUSMarketOpen() {
        ZonedDateTime nowET = ZonedDateTime.now(ZoneId.of("America/New_York"));

        DayOfWeek day = nowET.getDayOfWeek();
        LocalTime time = nowET.toLocalTime();

        // Market open/close times
        LocalTime marketOpen = LocalTime.of(9, 30);
        LocalTime marketClose = LocalTime.of(16, 0);

        // Check day is Monday–Friday
        boolean isWeekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;

        // Check time is between 9:30 AM and 4:00 PM
        boolean isInMarketHours = !time.isBefore(marketOpen) && !time.isAfter(marketClose);

        return isWeekday && isInMarketHours;
    }

}
