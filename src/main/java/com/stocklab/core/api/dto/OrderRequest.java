package com.stocklab.core.api.dto;

import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.order.OrderType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class OrderRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "stockCode is required")
    private String stockCode;

    @NotNull(message = "orderType is required")
    private OrderType orderType;

    @NotNull(message = "orderSide is required")
    private OrderSide orderSide;

    private BigDecimal price;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be positive")
    private BigDecimal quantity;

    @AssertTrue(message = "Limit orders require a positive price")
    public boolean isLimitPriceValid() {
        if (orderType == OrderType.MARKET) {
            return true;
        }
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }
}
