package com.abhishek.realtimeinsighthub.service.publisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.abhishek.realtimeinsighthub.dto.NewsDataDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;

@Service
public class NewsDataService implements MarketDataService {

    private final String[] STOCKS = {"AAPL", "TSLA", "MSFT", "GOOGL", "AMZN", "NVDA", "META"};
    private final String TOPIC = "news-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WebClient webClient;

    private final String NEWS_API_KEY;
    private final ObjectMapper objectMapper;

    public NewsDataService(KafkaTemplate<String, String> kafkaTemplate,
     @Value("${apikeys.news}") String newsApiKey, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.webClient = WebClient.builder().baseUrl("https://newsapi.org/v2").build();
        this.NEWS_API_KEY = newsApiKey;
        this.objectMapper = objectMapper;
    }

    @Override
    public void fetchAndPublishData() {
        for (String stock : STOCKS) {
            try {

                Map<String, Object> newsData = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/everything")
                                .queryParam("q", stock)
                                .queryParam("pageSize", 5)
                                .queryParam("sortBy", "publishedAt")
                                .queryParam("apiKey", NEWS_API_KEY)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                if (newsData == null || !newsData.containsKey("articles")) {
                    System.err.println("No news data for " + stock);
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> articles = (List<Map<String, Object>>) newsData.get("articles");
                StringBuilder combinedTextBuilder = new StringBuilder();

                for (Map<String, Object> article : articles) {
                    String title = (String) article.getOrDefault("title", "");
                    String description = (String) article.getOrDefault("description", "");
                    String combinedText = (title + " " + description).trim();
                    combinedTextBuilder.append(combinedText).append(". ");
                }
                NewsDataDto newsDataDto = new NewsDataDto(
                    Instant.now(),
                    stock,
                    combinedTextBuilder.toString()
                );

                String jsonMessage = objectMapper.writeValueAsString(newsDataDto);
                kafkaTemplate.send(TOPIC, jsonMessage);
                System.out.println("Published News Data: " + newsDataDto);

            } catch (Exception e) {
                System.err.println("Error fetching news data for " + stock + ": " + e.getMessage());
            }
        }
    }
}
