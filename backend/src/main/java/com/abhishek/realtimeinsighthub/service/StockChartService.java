package com.abhishek.realtimeinsighthub.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.abhishek.realtimeinsighthub.dto.StockChartDto;
import com.abhishek.realtimeinsighthub.entity.Price;
import com.abhishek.realtimeinsighthub.entity.Sentiment;
import com.abhishek.realtimeinsighthub.repo.PriceRepo;
import com.abhishek.realtimeinsighthub.repo.SentimentRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockChartService {

    private final PriceRepo priceRepo;
    private final SentimentRepo sentimentRepo;

    public List<StockChartDto> getChartData(String stock, Duration period) {
        List<Price> prices = priceRepo.findByStockAndTimestampAfterOrderByTimestampAsc(stock, period.toMillis() > 0 
            ? Instant.now().minus(period) 
            : Instant.EPOCH);
        List<Sentiment> sentiments = sentimentRepo.findByStockAndTimestampAfterOrderByTimestampAsc(stock, period.toMillis() > 0 
            ? Instant.now().minus(period) 
            : Instant.EPOCH);
        
        if (prices.isEmpty() && !sentiments.isEmpty()) {
            return sentiments.stream()
                .map(s -> new StockChartDto(
                    s.getTimestamp(),
                    null,
                    s.getSentiment(),
                    s.getCompoundScore()
                ))
                .collect(Collectors.toList());
        }

        NavigableMap<Instant, Sentiment> sentimentMap = sentiments.stream()
            .collect(Collectors.toMap(
                Sentiment::getTimestamp,
                Function.identity(),
                (s1, s2) -> s1,
                TreeMap::new
            ));
        
        return prices.stream()
            .map(p -> {
                Instant ts = p.getTimestamp();
                Map.Entry<Instant, Sentiment> floor = sentimentMap.floorEntry(ts);

                Sentiment closest = null;
                if (floor != null) {
                    closest = floor.getValue();
                }

                return new StockChartDto(
                    ts,
                    p.getPrice(),
                    closest != null ? closest.getSentiment() : null,
                    closest != null ? closest.getCompoundScore() : null
                );
            })
            .collect(Collectors.toList());
    }
}
