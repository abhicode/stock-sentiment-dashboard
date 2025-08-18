package com.abhishek.realtimeinsighthub.service.publisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.abhishek.realtimeinsighthub.dto.StockDataDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;

@Service
public class StockDataService implements MarketDataService{

    private final String[] STOCKS = {"AAPL", "TSLA", "MSFT", "GOOGL", "AMZN", "NVDA", "META"};
    private final String TOPIC = "stock-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper;
    private final WebClient webClient;
    private final String STOCK_API_KEY;

    public StockDataService(KafkaTemplate<String, String> kafkaTemplate, @Value("${apikeys.stock}") String stockApiKey,
        ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.STOCK_API_KEY = stockApiKey;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder().baseUrl("https://finnhub.io/api/v1/").build();
    }

    @Override
    public void fetchAndPublishData() {
        for (String stock : STOCKS) {
            try {
                
                Map<String, Object> stockData = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                        .path("/quote")
                        .queryParam("symbol", stock)
                        .queryParam("token", STOCK_API_KEY)
                        .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

                if (stockData == null || stockData.get("c") == null) {
                    System.err.println("No price data for stock: " + stock);
                    continue;
                }

                BigDecimal price = new BigDecimal(stockData.get("c").toString());

                StockDataDto stockDataDto = new StockDataDto(
                    Instant.now(),
                    stock,
                    price);

                String jsonMessage = objectMapper.writeValueAsString(stockDataDto);

                kafkaTemplate.send(TOPIC, jsonMessage);
                System.out.println("Published Stock Data: " + stockDataDto);
            } catch (Exception e) {
                System.err.println("Error fetching stock data for " + stock + ": " + e.getMessage());
            }
        }    
    }

}
