package com.stocklab.core.domain.order.service;

import com.stocklab.core.api.dto.OrderRequest;
import com.stocklab.core.config.lock.DistributedLock;
import com.stocklab.core.domain.auth.User;
import com.stocklab.core.domain.auth.repository.UserRepository;
import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.order.event.OrderEvent;
import com.stocklab.core.domain.order.repository.OrderRepository;
import com.stocklab.core.domain.portfolio.Portfolio;
import com.stocklab.core.domain.portfolio.UserStock;
import com.stocklab.core.domain.portfolio.repository.PortfolioRepository;
import com.stocklab.core.domain.portfolio.repository.UserStockRepository;
import com.stocklab.core.domain.stock.Stock;
import com.stocklab.core.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final PortfolioRepository portfolioRepository;
    private final UserStockRepository userStockRepository;
    private final OrderBookRedisRepository orderBookRedisRepository;
    private final ReservationReleaseService reservationReleaseService;
    private final StreamBridge streamBridge;

    @DistributedLock(key = "#request.userId")
    @Transactional
    public Long placeOrder(OrderRequest request) {
        // 1. 가용 자원 검증 (User, Stock, Portfolio)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Stock stock = stockRepository.findById(request.getStockCode())
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));
        Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        // 2. 가용 자원 예약 (매수: 현금 동결, 매도: 보유 수량 동결)
        if (request.getOrderSide() == OrderSide.BUY) {
            BigDecimal reserveAmount = request.getPrice().multiply(request.getQuantity());
            portfolio.freezeCash(reserveAmount);
        } else {
            UserStock userStock = userStockRepository
                    .findByUserIdAndStockCodeForUpdate(request.getUserId(), request.getStockCode())
                    .orElseThrow(() -> new IllegalStateException("Insufficient stock quantity"));
            userStock.freezeQuantity(request.getQuantity());
        }

        // 3. 주문 저장 (DB - PENDING 상태)
        Order order = Order.builder()
                .user(user)
                .stock(stock)
                .orderType(request.getOrderType())
                .orderSide(request.getOrderSide())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .build();
        
        Order savedOrder = orderRepository.save(order);

        // 4. Kafka 이벤트 발행 (비동기 파이프라인 시작)
        OrderEvent event = OrderEvent.builder()
                .orderId(savedOrder.getOrderId())
                .userId(user.getId())
                .stockCode(stock.getStockCode())
                .orderType(savedOrder.getOrderType())
                .orderSide(savedOrder.getOrderSide())
                .price(savedOrder.getPrice())
                .quantity(savedOrder.getQuantity())
                .build();

        boolean sent = streamBridge.send("orders-out-0", event);
        if (!sent) {
            log.error("Failed to send order event to Kafka: {}", event.getOrderId());
            throw new RuntimeException("Kafka message delivery failed");
        }

        return savedOrder.getOrderId();
    }

    @DistributedLock(key = "#userId")
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Order does not belong to user: " + userId);
        }
        if (!order.isCancelable()) {
            throw new IllegalStateException("Order cannot be canceled in status: " + order.getStatus());
        }

        reservationReleaseService.releaseRemaining(order);
        order.cancel();

        orderBookRedisRepository.removeOrder(
                order.getStock().getStockCode(),
                order.getOrderSide().name(),
                order.getOrderId()
        );

        log.info("Order canceled: orderId={}, userId={}", orderId, userId);
    }
}
