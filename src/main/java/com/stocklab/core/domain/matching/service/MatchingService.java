package com.stocklab.core.domain.matching.service;

import com.stocklab.core.domain.matching.Execution;
import com.stocklab.core.domain.matching.repository.ExecutionRepository;
import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderStatus;
import com.stocklab.core.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;
    private final OrderBookRedisRepository orderBookRedisRepository;

    @Transactional
    public void processMatch(String stockCode, Long buyOrderId, Long sellOrderId, BigDecimal matchPrice) {
        Order buyOrder = orderRepository.findById(buyOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Buy order not found: " + buyOrderId));
        Order sellOrder = orderRepository.findById(sellOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Sell order not found: " + sellOrderId));

        if (buyOrder.getStatus() == OrderStatus.COMPLETED || sellOrder.getStatus() == OrderStatus.COMPLETED) {
            log.warn("One of the orders is already completed. Skipping match.");
            return;
        }

        BigDecimal buyRemaining = buyOrder.getQuantity().subtract(buyOrder.getFilledQuantity());
        BigDecimal sellRemaining = sellOrder.getQuantity().subtract(sellOrder.getFilledQuantity());
        BigDecimal matchQuantity = buyRemaining.min(sellRemaining);

        // Update Orders
        buyOrder.fill(matchQuantity);
        sellOrder.fill(matchQuantity);

        // Create Execution
        Execution execution = Execution.builder()
                .order(buyOrder) // Using buy order as primary reference
                .buyer(buyOrder.getUser())
                .seller(sellOrder.getUser())
                .stock(buyOrder.getStock())
                .executionPrice(matchPrice)
                .executionQuantity(matchQuantity)
                .build();

        executionRepository.save(execution);

        // Update Redis if fully filled
        if (buyOrder.getStatus() == OrderStatus.COMPLETED) {
            orderBookRedisRepository.removeOrder(stockCode, "BUY", buyOrderId);
        }
        if (sellOrder.getStatus() == OrderStatus.COMPLETED) {
            orderBookRedisRepository.removeOrder(stockCode, "SELL", sellOrderId);
        }

        log.info("Match Executed: Stock={}, Price={}, Qty={}, BuyOrder={}, SellOrder={}",
                stockCode, matchPrice, matchQuantity, buyOrderId, sellOrderId);
    }
}
