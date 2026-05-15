package com.stocklab.core.domain.matching.orderbook;

import java.util.List;

public record OrderBookSnapshot(
        String stockCode,
        List<OrderBookLevel> bids,
        List<OrderBookLevel> asks
) {
}
