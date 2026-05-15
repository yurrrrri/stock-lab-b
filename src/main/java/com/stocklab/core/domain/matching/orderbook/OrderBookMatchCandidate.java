package com.stocklab.core.domain.matching.orderbook;

import java.math.BigDecimal;

public record OrderBookMatchCandidate(
        Long buyOrderId,
        Long sellOrderId,
        BigDecimal matchPrice
) {
}
