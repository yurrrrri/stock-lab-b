package com.stocklab.core.domain.matching.service;

import com.stocklab.core.domain.matching.Execution;
import com.stocklab.core.domain.matching.repository.ExecutionRepository;
import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderSide;
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
    private final SettlementService settlementService;

    @Transactional
    public void processMatch(String stockCode, Long buyOrderId, Long sellOrderId, BigDecimal matchPrice) {
        Order buyOrder;
        Order sellOrder;
        if (buyOrderId < sellOrderId) {
            buyOrder = loadOrderForUpdate(buyOrderId);
            sellOrder = loadOrderForUpdate(sellOrderId);
        } else {
            sellOrder = loadOrderForUpdate(sellOrderId);
            buyOrder = loadOrderForUpdate(buyOrderId);
        }

        if (!buyOrder.isCancelable() || !sellOrder.isCancelable()) {
            log.warn("Skipping match because an order is not active. buy={}, sell={}",
                    buyOrder.getStatus(), sellOrder.getStatus());
            removeFromOrderBookIfInactive(stockCode, buyOrder);
            removeFromOrderBookIfInactive(stockCode, sellOrder);
            return;
        }

        BigDecimal buyRemaining = buyOrder.getRemainingQuantity();
        BigDecimal sellRemaining = sellOrder.getRemainingQuantity();
        if (buyRemaining.compareTo(BigDecimal.ZERO) <= 0 || sellRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal matchQuantity = buyRemaining.min(sellRemaining);

        // Update Orders
        buyOrder.fill(matchQuantity);
        sellOrder.fill(matchQuantity);

        settlementService.settle(buyOrder, sellOrder, matchPrice, matchQuantity);

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

        orderBookRedisRepository.syncOrderInBook(stockCode, buyOrder);
        orderBookRedisRepository.syncOrderInBook(stockCode, sellOrder);

        log.info("Match Executed: Stock={}, Price={}, Qty={}, BuyOrder={}, SellOrder={}",
                stockCode, matchPrice, matchQuantity, buyOrderId, sellOrderId);
    }

    private Order loadOrderForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private void removeFromOrderBookIfInactive(String stockCode, Order order) {
        if (order.isCancelable()) {
            return;
        }
        orderBookRedisRepository.removeOrder(
                stockCode,
                order.getOrderSide() == OrderSide.BUY ? "BUY" : "SELL",
                order.getOrderId()
        );
    }
}
