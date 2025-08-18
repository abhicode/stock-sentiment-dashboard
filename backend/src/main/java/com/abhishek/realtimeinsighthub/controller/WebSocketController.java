package com.abhishek.realtimeinsighthub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.abhishek.realtimeinsighthub.dto.SentimentDataDto;
import com.abhishek.realtimeinsighthub.dto.StockDataDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    @Autowired
    private final SimpMessagingTemplate messagingTemplate;

    public void sendStockUpdate(StockDataDto stockData) {
        messagingTemplate.convertAndSend("/topic/stock", stockData);
        System.out.println("Successfully sent stock update");
    }

    public void sendSentimentUpdate(SentimentDataDto sentimentData) {
        messagingTemplate.convertAndSend("/topic/sentiment", sentimentData);
        System.out.println("Successfully sent sentiment update");
    }

    @GetMapping("/test-send")
    public String sendTestMessage() {
        messagingTemplate.convertAndSend("/topic/sentiment", "Hello");
        return "Test Message \"Hello\" sent to websocket topic \"sentiment\"";
    }
}
