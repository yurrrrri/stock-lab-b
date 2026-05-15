package com.stocklab.core.domain.order.service;

import com.stocklab.core.api.dto.OrderRequest;
import com.stocklab.core.api.exception.AccessDeniedException;
import com.stocklab.core.api.exception.MessagingException;
import com.stocklab.core.config.lock.DistributedLock;
import com.stocklab.core.domain.auth.User;
import com.stocklab.core.domain.auth.repository.UserRepository;
import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.order.event.OrderEvent;
import com.stocklab.core.domain.order.messaging.OrderEventPublisher;
import com.stocklab.core.domain.order.repository.OrderRepository;
import com.stocklab.core.domain.portfolio.Portfolio;
import com.stocklab.core.domain.portfolio.UserStock;
import com.stocklab.core.domain.portfolio.repository.PortfolioRepository;
import com.stocklab.core.domain.portfolio.repository.UserStockRepository;
import com.stocklab.core.domain.stock.Stock;
import com.stocklab.core.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final OrderEventPublisher orderEventPublisher;
    private final MarketOrderPriceResolver marketOrderPriceResolver;

    @DistributedLock(key = "#authenticatedUserId")
    @Transactional
    public Long placeOrder(Long authenticatedUserId, OrderRequest request) {
        assertMatchingUser(authenticatedUserId, request.getUserId());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Stock stock = stockRepository.findById(request.getStockCode())
                .orElseThrow(() -> new IllegalArgumentException("Stock not found"));

        BigDecimal effectivePrice = marketOrderPriceResolver.resolve(
                request.getStockCode(),
                request.getOrderType(),
                request.getOrderSide(),
                request.getPrice()
        );

        Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));

        if (request.getOrderSide() == OrderSide.BUY) {
            BigDecimal reserveAmount = effectivePrice.multiply(request.getQuantity());
            portfolio.freezeCash(reserveAmount);
        } else {
            UserStock userStock = userStockRepository
                    .findByUserIdAndStockCodeForUpdate(request.getUserId(), request.getStockCode())
                    .orElseThrow(() -> new IllegalStateException("Insufficient stock quantity"));
            userStock.freezeQuantity(request.getQuantity());
        }

        Order order = Order.builder()
                .user(user)
                .stock(stock)
                .orderType(request.getOrderType())
                .orderSide(request.getOrderSide())
                .price(effectivePrice)
                .quantity(request.getQuantity())
                .build();

        Order savedOrder = orderRepository.save(order);

        OrderEvent event = OrderEvent.builder()
                .orderId(savedOrder.getOrderId())
                .userId(user.getId())
                .stockCode(stock.getStockCode())
                .orderType(savedOrder.getOrderType())
                .orderSide(savedOrder.getOrderSide())
                .price(savedOrder.getPrice())
                .quantity(savedOrder.getQuantity())
                .build();

        boolean sent = orderEventPublisher.publish(event);
        if (!sent) {
            log.error("Failed to send order event to Kafka: {}", event.getOrderId());
            throw new MessagingException("Kafka message delivery failed");
        }

        return savedOrder.getOrderId();
    }

    @DistributedLock(key = "#authenticatedUserId")
    @Transactional
    public void cancelOrder(Long orderId, Long authenticatedUserId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getUser().getId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("Order does not belong to authenticated user");
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

        log.info("Order canceled: orderId={}, userId={}", orderId, authenticatedUserId);
    }

    private void assertMatchingUser(Long authenticatedUserId, Long requestUserId) {
        if (!authenticatedUserId.equals(requestUserId)) {
            throw new AccessDeniedException("Request userId does not match authenticated user");
        }
    }
}
