package com.stocklab.core.domain.order.service;

import com.stocklab.core.domain.matching.service.MatchingEngineService;
import com.stocklab.core.domain.order.event.OrderEvent;
import com.stocklab.core.domain.order.inbox.OrderEventInbox;
import com.stocklab.core.domain.order.inbox.OrderEventInboxStatus;
import com.stocklab.core.domain.order.inbox.repository.OrderEventInboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventProcessor {

    private final OrderEventInboxRepository orderEventInboxRepository;
    private final MatchingEngineService matchingEngineService;

    @Transactional
    public void process(OrderEvent event) {
        Long orderId = event.getOrderId();
        OrderEventInbox inbox = orderEventInboxRepository.findById(orderId).orElse(null);

        if (inbox != null && inbox.getStatus() == OrderEventInboxStatus.COMPLETED) {
            log.info("Skipping duplicate order event: orderId={}", orderId);
            return;
        }

        if (inbox == null) {
            inbox = registerInbox(orderId);
            if (inbox == null) {
                inbox = orderEventInboxRepository.findById(orderId)
                        .orElseThrow(() -> new IllegalStateException("Order event inbox missing after concurrent registration: " + orderId));
                if (inbox.getStatus() == OrderEventInboxStatus.COMPLETED) {
                    log.info("Skipping duplicate order event after concurrent completion: orderId={}", orderId);
                    return;
                }
            }
        }

        matchingEngineService.handleOrderEvent(event);
        inbox.markCompleted();
        log.info("Order event processed: orderId={}", orderId);
    }

    private OrderEventInbox registerInbox(Long orderId) {
        try {
            return orderEventInboxRepository.save(OrderEventInbox.processing(orderId));
        } catch (DataIntegrityViolationException ex) {
            log.debug("Concurrent order event registration detected: orderId={}", orderId);
            return null;
        }
    }
}
