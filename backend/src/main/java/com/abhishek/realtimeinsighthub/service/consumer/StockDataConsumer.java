package com.abhishek.realtimeinsighthub.service.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.abhishek.realtimeinsighthub.controller.WebSocketController;
import com.abhishek.realtimeinsighthub.dto.StockDataDto;
import com.abhishek.realtimeinsighthub.entity.Price;
import com.abhishek.realtimeinsighthub.repo.PriceRepo;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class StockDataConsumer implements ConsumerService {

    private final String TOPIC = "stock-topic";
    private final String GROUP_ID = "stock-consumer";

    private final PriceRepo priceRepo;
    private final ObjectMapper objectMapper;

    @Autowired
    private WebSocketController webSocketController;

    public StockDataConsumer(PriceRepo priceRepo) {
        this.priceRepo = priceRepo;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    @KafkaListener(topics = TOPIC, groupId = GROUP_ID)
    public void consume(String messageJson) {
        try {
            StockDataDto dto = objectMapper.readValue(messageJson, StockDataDto.class);

            Price price = new Price();
            price.setTimestamp(dto.getTimestamp());
            price.setStock(dto.getStock());
            price.setPrice(dto.getPrice());

            priceRepo.save(price);

            webSocketController.sendStockUpdate(dto);

        } catch (Exception e) {
            System.err.println("Failed to process stock message: " + e.getMessage());
            e.printStackTrace();
        }
        
    }

}
