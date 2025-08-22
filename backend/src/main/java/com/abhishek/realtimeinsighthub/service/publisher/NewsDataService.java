package com.abhishek.realtimeinsighthub.service.publisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.abhishek.realtimeinsighthub.dto.NewsDataDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

@Service
public class NewsDataService implements MarketDataService {

    private final String[] STOCKS = {"AAPL", "TSLA", "MSFT", "GOOGL", "AMZN", "NVDA", "META"};
    private final String TOPIC = "news-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    private final Map<String, Instant> lastSeen ;

    private final RateLimiter rateLimiter;

    public NewsDataService(KafkaTemplate<String, String> kafkaTemplate,
        ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.webClient = WebClient.builder().baseUrl("https://api.tickertick.com")
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.SO_KEEPALIVE, false)
                    .keepAlive(false) // do not keep connections alive
            )).build();
        this.objectMapper = objectMapper;
        this.lastSeen = new ConcurrentHashMap<>();
        this.rateLimiter = RateLimiter.create(10);
    }

    @Override
    public void fetchAndPublishData() {
        for (String stock : STOCKS) {
            try {
                rateLimiter.acquire();
                Map<String, Object> newsData = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/feed")
                                .queryParam("q", String.format("tt:%s", stock.toLowerCase()))
                                .queryParam("n", 5)
                                .build())
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                if (newsData == null || !newsData.containsKey("stories")) {
                    System.err.println("No news data for " + stock);
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> articles = (List<Map<String, Object>>) newsData.get("stories");

                Instant last = lastSeen.getOrDefault(stock, Instant.MIN);

                List<Map<String, Object>> newArticles = articles.stream()
                    .filter(article -> {
                        Object timeValue = article.get("time");
                        Instant publishedAt = null;

                        if (timeValue instanceof Number) {
                            publishedAt = Instant.ofEpochMilli(((Number) timeValue).longValue());
                        } else if (timeValue instanceof String) {
                            publishedAt = Instant.parse((String) timeValue);
                        }

                        return publishedAt != null && publishedAt.isAfter(last);
                        })
                    .toList();

                if (!newArticles.isEmpty()) {
                    // Find the maximum timestamp among the new articles
                    Instant newestTime = newArticles.stream()
                        .map(article -> {
                            Object timeValue = article.get("time");
                            if (timeValue instanceof Number) {
                                return Instant.ofEpochMilli(((Number) timeValue).longValue());
                            } else if (timeValue instanceof String) {
                                return Instant.parse((String) timeValue);
                            }
                            return Instant.MIN;
                        })
                        .max(Instant::compareTo)
                        .orElse(last);
                        
                    lastSeen.put(stock, newestTime);

                    for (Map<String, Object> article : newArticles) {
                        String title = (String) article.getOrDefault("title", "");
                        String description = (String) article.getOrDefault("description", "");
                        String text = (title + " " + description).trim();

                        Object articleTime = article.get("time");
                        Instant publishedAt;
                        if (articleTime instanceof Number) {
                            publishedAt = Instant.ofEpochMilli(((Number) articleTime).longValue());
                        } else if (articleTime instanceof String) {
                            publishedAt = Instant.parse((String) articleTime);
                        } else {
                            publishedAt = Instant.now();
                        }

                        NewsDataDto dto = new NewsDataDto(
                            publishedAt,
                            stock,
                            text
                        );

                        kafkaTemplate.send(TOPIC, objectMapper.writeValueAsString(dto));
                        System.out.println("Published News Data: " + dto);
                    }
                } else {
                    System.out.println("No new articles for: " + stock);
                }
            } catch (Exception e) {
                System.err.println("Error fetching news data for " + stock + ": " + e.getMessage());
            }
        }
    }
}
