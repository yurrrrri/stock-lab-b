package com.stocklab.core.domain.order.service;

import com.stocklab.core.api.exception.NoLiquidityException;
import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MarketOrderPriceResolver {

    private final OrderBookRedisRepository orderBookRedisRepository;

    public BigDecimal resolve(String stockCode, OrderType orderType, OrderSide orderSide, BigDecimal limitPrice) {
        if (orderType == OrderType.LIMIT) {
            return limitPrice;
        }

        BigDecimal bestOppositePrice = orderBookRedisRepository.getBestOppositePrice(stockCode, orderSide)
                .orElseThrow(() -> new NoLiquidityException(
                        "No opposite-side liquidity for market order on stock: " + stockCode
                ));

        if (orderSide == OrderSide.BUY) {
            // Apply 5% buffer for market buy orders to handle slippage
            return bestOppositePrice.multiply(BigDecimal.valueOf(1.05));
        }

        return bestOppositePrice;
    }
}
