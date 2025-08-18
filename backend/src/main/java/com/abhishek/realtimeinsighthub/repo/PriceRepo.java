package com.abhishek.realtimeinsighthub.repo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.abhishek.realtimeinsighthub.entity.Price;

@Repository
public interface PriceRepo extends JpaRepository<Price, Long> {
    List<Price> findByStockOrderByTimestampAsc(String stock);
    
    List<Price> findByStockAndTimestampAfterOrderByTimestampAsc(String stock, Instant after);

    int deleteByTimestampBefore(Instant cutoff);
}
