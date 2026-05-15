package com.stocklab.core.domain.order.service;

import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.OrderSide;
import com.stocklab.core.domain.portfolio.Portfolio;
import com.stocklab.core.domain.portfolio.UserStock;
import com.stocklab.core.domain.portfolio.repository.PortfolioRepository;
import com.stocklab.core.domain.portfolio.repository.UserStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReservationReleaseService {

    private final PortfolioRepository portfolioRepository;
    private final UserStockRepository userStockRepository;

    public void releaseRemaining(Order order) {
        BigDecimal remaining = order.getRemainingQuantity();
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Long userId = order.getUser().getId();
        if (order.getOrderSide() == OrderSide.BUY) {
            Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(userId)
                    .orElseThrow(() -> new IllegalStateException("Portfolio not found for user: " + userId));
            portfolio.unfreezeCash(order.getPrice().multiply(remaining));
        } else {
            String stockCode = order.getStock().getStockCode();
            UserStock userStock = userStockRepository.findByUserIdAndStockCodeForUpdate(userId, stockCode)
                    .orElseThrow(() -> new IllegalStateException(
                            "User stock not found for user " + userId + " and stock " + stockCode));
            userStock.unfreezeQuantity(remaining);
        }
    }
}
