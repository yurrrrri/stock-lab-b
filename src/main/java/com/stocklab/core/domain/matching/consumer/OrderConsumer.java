package com.stocklab.core.domain.matching.consumer;

import com.stocklab.core.domain.matching.repository.OrderBookRedisRepository;
import com.stocklab.core.domain.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderBookRedisRepository orderBookRedisRepository;

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
        // TODO: 매수/매도 호가를 비교하여 체결 처리 로직 구현
        // 1. BUY ZSET의 최고가와 SELL ZSET의 최저가 비교
        // 2. BUY Price >= SELL Price 이면 체결(Execution) 생성
        // 3. 체결 완료 시 Redis 및 DB 업데이트
    }
}
