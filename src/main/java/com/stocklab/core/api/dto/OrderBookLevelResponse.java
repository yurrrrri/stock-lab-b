package com.stocklab.core.api.dto;

import java.math.BigDecimal;

public record OrderBookLevelResponse(
        Long orderId,
        BigDecimal price,
        BigDecimal quantity
) {
}
