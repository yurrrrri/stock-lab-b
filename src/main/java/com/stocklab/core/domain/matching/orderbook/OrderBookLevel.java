package com.stocklab.core.domain.matching.orderbook;

import java.math.BigDecimal;

public record OrderBookLevel(
        Long orderId,
        BigDecimal price,
        BigDecimal quantity
) {
}
