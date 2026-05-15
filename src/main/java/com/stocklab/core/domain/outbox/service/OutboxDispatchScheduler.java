package com.stocklab.core.domain.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocklab.core.domain.order.event.OrderEvent;
import com.stocklab.core.domain.order.messaging.OrderEventPublisher;
import com.stocklab.core.domain.outbox.OutboxEventType;
import com.stocklab.core.domain.outbox.OutboxMessage;
import com.stocklab.core.domain.outbox.OutboxStatus;
import com.stocklab.core.domain.outbox.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxDispatchScheduler {

    private final OutboxMessageRepository outboxMessageRepository;
    private final OrderEventPublisher orderEventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${stocklab.outbox.poll-interval-ms:1000}")
    @Transactional
    public void dispatchPendingMessages() {
        List<OutboxMessage> pendingMessages = outboxMessageRepository.findPendingForDispatch(
                OutboxStatus.PENDING,
                PageRequest.of(0, 50)
        );
        if (pendingMessages.isEmpty()) {
            return;
        }

        for (OutboxMessage message : pendingMessages) {
            dispatch(message);
        }
    }

    private void dispatch(OutboxMessage message) {
        try {
            if (message.getEventType() != OutboxEventType.ORDER_PLACED) {
                message.markFailed("Unsupported outbox event type: " + message.getEventType());
                return;
            }

            OrderEvent event = objectMapper.readValue(message.getPayload(), OrderEvent.class);
            boolean sent = orderEventPublisher.publish(event);
            if (!sent) {
                message.markFailed("Kafka publish returned false");
                log.warn("Outbox dispatch failed: id={}", message.getId());
                return;
            }

            message.markPublished();
            log.debug("Outbox message published: id={}, orderId={}", message.getId(), event.getOrderId());
        } catch (Exception ex) {
            message.markFailed(ex.getMessage());
            log.error("Outbox dispatch error: id={}", message.getId(), ex);
        }
    }
}
