package com.stocklab.core.domain.matching.service;

import com.stocklab.core.api.dto.OrderBookLevelResponse;
import com.stocklab.core.api.dto.OrderBookResponse;
import com.stocklab.core.domain.matching.orderbook.OrderBookLevel;
import com.stocklab.core.domain.matching.orderbook.OrderBookSnapshot;
import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderBookQueryService {

    private final StockRepository stockRepository;
    private final OrderBookRedisRepository orderBookRedisRepository;

    public OrderBookResponse getOrderBook(String stockCode, int depth) {
        stockRepository.findById(stockCode)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockCode));

        OrderBookSnapshot snapshot = orderBookRedisRepository.getSnapshot(stockCode, depth);
        return new OrderBookResponse(
                snapshot.stockCode(),
                toResponses(snapshot.bids()),
                toResponses(snapshot.asks())
        );
    }

    private List<OrderBookLevelResponse> toResponses(List<OrderBookLevel> levels) {
        return levels.stream()
                .map(level -> new OrderBookLevelResponse(level.orderId(), level.price(), level.quantity()))
                .toList();
    }
}
