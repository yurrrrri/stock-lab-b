package com.stocklab.core.domain.order.service;

import com.stocklab.core.api.dto.OrderRequest;
import com.stocklab.core.config.lock.DistributedLock;
import com.stocklab.core.domain.auth.User;
import com.stocklab.core.domain.auth.repository.UserRepository;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.order.event.OrderEvent;
import com.stocklab.core.domain.order.repository.OrderRepository;
import com.stocklab.core.domain.portfolio.Portfolio;
import com.stocklab.core.domain.portfolio.repository.PortfolioRepository;
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
    private final StreamBridge streamBridge;

    @DistributedLock(key = "#request.userId")
    @Transactional
    public Long placeOrder(OrderRequest request) {
        // 1. 가용 자원 검증 (User, Stock, Portfolio)
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Stock stock = stockRepository.findById(request.getStockCode())
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));
        Portfolio portfolio = portfolioRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        // 2. 가용 잔고 검증 (매수 시)
        if (request.getOrderSide() == OrderSide.BUY) {
            BigDecimal requiredAmount = request.getPrice().multiply(request.getQuantity());
            if (portfolio.getCashBalance().compareTo(requiredAmount) < 0) {
                throw new IllegalStateException("Insufficient cash balance");
            }
            // 실제 차감은 체결 시점에 하거나, 주문 시점에 예약(Freeze) 처리를 할 수 있으나 
            // 여기서는 단순 가검증 후 진행합니다.
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
}
