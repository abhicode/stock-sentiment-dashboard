package com.abhishek.realtimeinsighthub.service.consumer;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.abhishek.realtimeinsighthub.controller.WebSocketController;
import com.abhishek.realtimeinsighthub.dto.NewsDataDto;
import com.abhishek.realtimeinsighthub.dto.SentimentDataDto;
import com.abhishek.realtimeinsighthub.dto.SentimentResponseDto;
import com.abhishek.realtimeinsighthub.entity.Sentiment;
import com.abhishek.realtimeinsighthub.repo.SentimentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SentimentDataConsumer implements ConsumerService {

    private final String TOPIC = "news-topic";
    private final String GROUP_ID = "news-consumer";

    private final SentimentRepo sentimentRepo;
    private final ObjectMapper objectMapper;

    private final List<NewsDataDto> buffer = new ArrayList<>();
    private final Object lock = new Object();
    private final int BATCH_SIZE = 10;
    private final Duration FLUSH_INTERVAL = Duration.ofSeconds(5);

    private final WebClient webClient;

    @Autowired
    private WebSocketController webSocketController;

    public SentimentDataConsumer(SentimentRepo sentimentRepo, @Value("${fastapi.url}") String fastApiUrl) {
        this.sentimentRepo = sentimentRepo;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        this.webClient = WebClient.builder()
                .baseUrl(fastApiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::flushBuffer
        , FLUSH_INTERVAL.toSeconds(), FLUSH_INTERVAL.toSeconds(), TimeUnit.SECONDS);
    }

    @Override
    @KafkaListener(topics = TOPIC, groupId = GROUP_ID)
    public void consume(String messageJson) {
        try {
            NewsDataDto dto = objectMapper.readValue(messageJson, NewsDataDto.class);
            synchronized (lock) {
                buffer.add(dto);
                if (buffer.size() >= BATCH_SIZE) {
                    flushBuffer();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process sentiment message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void flushBuffer() {
        List<NewsDataDto> batch;
        synchronized (lock) {
            if (buffer.isEmpty()) return;
            batch = mergeNewsWithEqualTimestamp(new ArrayList<>(buffer));
            buffer.clear();
        }

        try {
            List<SentimentResponseDto> results = webClient.post()
                    .uri("")
                    .bodyValue(batch)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<SentimentResponseDto>>() {})
                    .block();

            List<Sentiment> entities = results.stream()
                .map(this::mapResponseToEntity)
                .toList();

            sentimentRepo.saveAll(entities);
            
            for (Sentiment sentimentEntity : entities) {
                SentimentDataDto sentimentData = new SentimentDataDto(
                    sentimentEntity.getTimestamp(),
                    sentimentEntity.getStock(),
                    sentimentEntity.getSentiment(), 
                    sentimentEntity.getCompoundScore()
                );
                webSocketController.sendSentimentUpdate(sentimentData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Sentiment mapResponseToEntity(SentimentResponseDto response) {
        Sentiment s = new Sentiment();
        s.setTimestamp(response.getTimestamp());
        s.setStock(response.getStock());
        s.setSentiment(response.getSentiment());
        s.setNegScore(response.getScores().getOrDefault("neg", 0.0));
        s.setNeuScore(response.getScores().getOrDefault("neu", 0.0));
        s.setPosScore(response.getScores().getOrDefault("pos", 0.0));
        s.setCompoundScore(response.getScores().getOrDefault("compound", 0.0));
        return s;
    }

    private static List<NewsDataDto> mergeNewsWithEqualTimestamp(List<NewsDataDto> newsList) {
        return newsList.stream()
            .collect(Collectors.groupingBy(
                n -> Arrays.asList(n.getStock(), n.getTimestamp()), // group key
                Collectors.mapping(NewsDataDto::getNewsData, Collectors.toList())
            ))
            .entrySet()
            .stream()
            .map(entry -> new NewsDataDto(
                    (Instant) entry.getKey().get(1), // timestamp
                    (String) entry.getKey().get(0), // stock
                    String.join(" ", 
                    entry.getValue().stream().map(Object::toString).toList())
                    ))
            .toList();
    }
}
