package com.stocklab.core.domain.order.messaging;

import com.stocklab.core.domain.order.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final String BINDING = "orders-out-0";

    private final StreamBridge streamBridge;

    public boolean publish(OrderEvent event) {
        return streamBridge.send(
                BINDING,
                MessageBuilder.withPayload(event)
                        .setHeader(KafkaHeaders.KEY, event.getStockCode())
                        .build()
        );
    }
}
