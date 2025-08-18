package com.abhishek.realtimeinsighthub.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.abhishek.realtimeinsighthub.repo.PriceRepo;
import com.abhishek.realtimeinsighthub.repo.SentimentRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataCleanupService {

    private final PriceRepo priceRepo;
    private final SentimentRepo sentimentRepo;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOldRecords() {
        Instant cutOff = Instant.now().minus(30, ChronoUnit.DAYS);

        int deletedPrices = priceRepo.deleteByTimestampBefore(cutOff);
        int deletedSentiments = sentimentRepo.deleteByTimestampBefore(cutOff);

        System.out.println("Cleanup done: " + deletedPrices + " prices, " 
                            + deletedSentiments + " sentiments deleted.");
    }
}
