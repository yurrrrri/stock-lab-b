package com.stocklab.core.domain.matching.consumer;

import com.stocklab.core.domain.order.event.OrderEvent;
import com.stocklab.core.domain.order.service.OrderEventProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderEventProcessor orderEventProcessor;

    @Bean
    public Consumer<OrderEvent> consumeOrder() {
        return event -> {
            log.info("Received order event: orderId={}, stock={}", event.getOrderId(), event.getStockCode());
            orderEventProcessor.process(event);
        };
    }
}
