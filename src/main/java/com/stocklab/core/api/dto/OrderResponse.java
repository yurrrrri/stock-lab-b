package com.stocklab.core.api.dto;

import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.order.OrderStatus;
import com.stocklab.core.domain.order.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        Long userId,
        String stockCode,
        OrderType orderType,
        OrderSide orderSide,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal filledQuantity,
        BigDecimal remainingQuantity,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getUser().getId(),
                order.getStock().getStockCode(),
                order.getOrderType(),
                order.getOrderSide(),
                order.getPrice(),
                order.getQuantity(),
                order.getFilledQuantity(),
                order.getRemainingQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
