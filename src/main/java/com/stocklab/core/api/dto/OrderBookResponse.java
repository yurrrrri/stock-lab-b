package com.stocklab.core.api.dto;

import java.util.List;

public record OrderBookResponse(
        String stockCode,
        List<OrderBookLevelResponse> bids,
        List<OrderBookLevelResponse> asks
) {
}
