package com.abhishek.realtimeinsighthub.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "sentiments")
public class Sentiment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant timestamp;

    @Column(length = 10)
    private String stock;

    private String sentiment;

    @Column(name = "neg_score")
    private Double negScore;

    @Column(name = "neu_score")
    private Double neuScore;

    @Column(name = "pos_score")
    private Double posScore;

    @Column(name = "compound_score")
    private Double compoundScore;
}
