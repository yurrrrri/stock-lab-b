package com.stocklab.core.domain.matching.consumer;

import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.matching.service.MatchingService;
import com.stocklab.core.domain.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderBookRedisRepository orderBookRedisRepository;
    private final MatchingService matchingService;

    /**
     * Kafka Consumer for Order Events.
     * Consumes from 'order-topic' and triggers matching logic.
     */
    @Bean
    public Consumer<OrderEvent> consumeOrder() {
        return event -> {
            log.info("Received Order Event: {}", event.getOrderId());

            // 1. Redis 호가창에 주문 적재 (ZSET)
            orderBookRedisRepository.addOrder(
                    event.getStockCode(),
                    event.getOrderSide().name(),
                    event.getOrderId(),
                    event.getPrice()
            );

            // 2. 매칭 엔진 구동 (핵심 매칭 로직 호출)
            match(event.getStockCode());
        };
    }

    private void match(String stockCode) {
        log.info("Triggering Matching Engine for stock: {}", stockCode);
        
        while (true) {
            Set<ZSetOperations.TypedTuple<String>> bestBuy = orderBookRedisRepository.getBestBuyOrders(stockCode, 1);
            Set<ZSetOperations.TypedTuple<String>> bestSell = orderBookRedisRepository.getBestSellOrders(stockCode, 1);

            if (bestBuy == null || bestBuy.isEmpty() || bestSell == null || bestSell.isEmpty()) {
                break;
            }

            ZSetOperations.TypedTuple<String> buyEntry = bestBuy.iterator().next();
            ZSetOperations.TypedTuple<String> sellEntry = bestSell.iterator().next();

            if (buyEntry.getScore() == null || sellEntry.getScore() == null) {
                break;
            }

            BigDecimal buyPrice = BigDecimal.valueOf(buyEntry.getScore());
            BigDecimal sellPrice = BigDecimal.valueOf(sellEntry.getScore());

            // BUY Price >= SELL Price 이면 체결
            if (buyPrice.compareTo(sellPrice) >= 0) {
                Long buyOrderId = Long.valueOf(buyEntry.getValue());
                Long sellOrderId = Long.valueOf(sellEntry.getValue());

                // 체결 가격은 보통 호가창에 먼저 등록된 주문(Maker)의 가격을 따름
                // 여기서는 간단히 매도 호가(Sell Price)를 체결가로 설정
                matchingService.processMatch(stockCode, buyOrderId, sellOrderId, sellPrice);
            } else {
                break;
            }
        }
    }
}
