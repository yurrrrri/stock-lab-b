package com.stocklab.core.domain.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocklab.core.domain.order.event.OrderEvent;
import com.stocklab.core.domain.outbox.OutboxEventType;
import com.stocklab.core.domain.outbox.OutboxMessage;
import com.stocklab.core.domain.outbox.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueueOrderPlaced(OrderEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            outboxMessageRepository.save(
                    OutboxMessage.pending(OutboxEventType.ORDER_PLACED, payload, event.getStockCode())
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order event for outbox", e);
        }
    }
}
