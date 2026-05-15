package com.stocklab.core.domain.matching.service;

import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.portfolio.Portfolio;
import com.stocklab.core.domain.portfolio.UserStock;
import com.stocklab.core.domain.portfolio.repository.PortfolioRepository;
import com.stocklab.core.domain.portfolio.repository.UserStockRepository;
import com.stocklab.core.domain.stock.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final PortfolioRepository portfolioRepository;
    private final UserStockRepository userStockRepository;

    @Transactional
    public void settle(Order buyOrder, Order sellOrder, BigDecimal matchPrice, BigDecimal matchQuantity) {
        Long buyerId = buyOrder.getUser().getId();
        Long sellerId = sellOrder.getUser().getId();
        Stock stock = buyOrder.getStock();
        String stockCode = stock.getStockCode();

        BigDecimal actualCost = matchPrice.multiply(matchQuantity);
        BigDecimal buyReservation = buyOrder.getPrice().multiply(matchQuantity);

        if (buyerId.equals(sellerId)) {
            Portfolio portfolio = loadPortfolioForUpdate(buyerId);
            portfolio.settleBuyReservation(buyReservation, actualCost);
            portfolio.receiveSaleProceeds(actualCost);

            UserStock userStock = loadUserStockForUpdate(buyerId, stockCode);
            userStock.settleSellReservation(matchQuantity);
            userStock.addQuantity(matchQuantity, matchPrice);
            return;
        }

        Portfolio buyerPortfolio;
        Portfolio sellerPortfolio;
        if (buyerId < sellerId) {
            buyerPortfolio = loadPortfolioForUpdate(buyerId);
            sellerPortfolio = loadPortfolioForUpdate(sellerId);
        } else {
            sellerPortfolio = loadPortfolioForUpdate(sellerId);
            buyerPortfolio = loadPortfolioForUpdate(buyerId);
        }

        buyerPortfolio.settleBuyReservation(buyReservation, actualCost);
        sellerPortfolio.receiveSaleProceeds(actualCost);

        UserStock sellerStock = loadUserStockForUpdate(sellerId, stockCode);
        sellerStock.settleSellReservation(matchQuantity);

        UserStock buyerStock = userStockRepository.findByUserIdAndStockCodeForUpdate(buyerId, stockCode)
                .orElseGet(() -> userStockRepository.save(
                        UserStock.builder()
                                .user(buyOrder.getUser())
                                .stock(stock)
                                .quantity(BigDecimal.ZERO)
                                .averagePrice(BigDecimal.ZERO)
                                .build()
                ));
        buyerStock.addQuantity(matchQuantity, matchPrice);
    }

    private Portfolio loadPortfolioForUpdate(Long userId) {
        return portfolioRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Portfolio not found for user: " + userId));
    }

    private UserStock loadUserStockForUpdate(Long userId, String stockCode) {
        return userStockRepository.findByUserIdAndStockCodeForUpdate(userId, stockCode)
                .orElseThrow(() -> new IllegalStateException(
                        "User stock not found for user " + userId + " and stock " + stockCode));
    }
}
