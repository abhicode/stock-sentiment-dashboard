package com.abhishek.realtimeinsighthub.repo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.abhishek.realtimeinsighthub.entity.Sentiment;

@Repository
public interface SentimentRepo extends JpaRepository<Sentiment, Long> {
    List<Sentiment> findByStockOrderByTimestampAsc(String stock);

    List<Sentiment> findByStockAndTimestampAfterOrderByTimestampAsc(String stock, Instant after);

    int deleteByTimestampBefore(Instant cutoff);
}
