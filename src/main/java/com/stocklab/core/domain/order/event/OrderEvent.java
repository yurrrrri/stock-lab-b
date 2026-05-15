package com.stocklab.core.domain.order.event;

import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.order.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEvent {
    private Long orderId;
    private Long userId;
    private String stockCode;
    private OrderType orderType;
    private OrderSide orderSide;
    private BigDecimal price;
    private BigDecimal quantity;
}
