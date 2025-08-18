package com.abhishek.realtimeinsighthub.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {
 @Bean
    public NewTopic stockTopic() {
        return TopicBuilder.name("stock-data")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic newsTopic() {
        return TopicBuilder.name("news-data")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
