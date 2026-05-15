package com.stocklab.core.domain.stock;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(nullable = false)
    private String stockName;

    @Column(nullable = false)
    private String marketType; // KOSPI, KOSDAQ, etc.

    @Column(nullable = false)
    private Boolean isTrading;

    @Builder
    public Stock(String stockCode, String stockName, String marketType, Boolean isTrading) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.marketType = marketType;
        this.isTrading = isTrading != null ? isTrading : true;
    }
}
