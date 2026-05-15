package com.stocklab.core.api.dto;

import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.order.OrderType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class OrderRequest {
    private Long userId;
    private String stockCode;
    private OrderType orderType;
    private OrderSide orderSide;
    private BigDecimal price;
    private BigDecimal quantity;
}
