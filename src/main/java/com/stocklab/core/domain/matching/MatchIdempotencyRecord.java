package com.stocklab.core.domain.matching;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_idempotency_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchIdempotencyRecord {

    @Id
    @Column(length = 120)
    private String matchKey;

    @Column(nullable = false)
    private Long buyOrderId;

    @Column(nullable = false)
    private Long sellOrderId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static MatchIdempotencyRecord create(String matchKey, Long buyOrderId, Long sellOrderId) {
        MatchIdempotencyRecord record = new MatchIdempotencyRecord();
        record.matchKey = matchKey;
        record.buyOrderId = buyOrderId;
        record.sellOrderId = sellOrderId;
        record.createdAt = LocalDateTime.now();
        return record;
    }
}
