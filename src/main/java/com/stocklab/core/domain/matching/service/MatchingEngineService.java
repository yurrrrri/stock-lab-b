package com.stocklab.core.domain.matching.service;

import com.stocklab.core.domain.matching.orderbook.OrderBookMatchCandidate;
import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEngineService {

    private final StockMatchLockExecutor stockMatchLockExecutor;
    private final OrderBookRedisRepository orderBookRedisRepository;
    private final MatchingService matchingService;

    public void handleOrderEvent(OrderEvent event) {
        String stockCode = event.getStockCode();
        stockMatchLockExecutor.execute(stockCode, () -> {
            orderBookRedisRepository.addOrder(
                    stockCode,
                    event.getOrderSide().name(),
                    event.getOrderId(),
                    event.getPrice(),
                    event.getQuantity()
            );
            runMatching(stockCode);
        });
    }

    private void runMatching(String stockCode) {
        while (true) {
            Optional<OrderBookMatchCandidate> candidateOptional =
                    orderBookRedisRepository.findCrossingMatch(stockCode);
            if (candidateOptional.isEmpty()) {
                break;
            }
            OrderBookMatchCandidate candidate = candidateOptional.get();

            log.info("Matching candidate found: stock={}, buy={}, sell={}, price={}",
                    stockCode, candidate.buyOrderId(), candidate.sellOrderId(), candidate.matchPrice());

            matchingService.processMatch(
                    stockCode,
                    candidate.buyOrderId(),
                    candidate.sellOrderId(),
                    candidate.matchPrice()
            );
        }
    }
}
