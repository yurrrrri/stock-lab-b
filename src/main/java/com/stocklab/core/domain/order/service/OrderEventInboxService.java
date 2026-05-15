package com.stocklab.core.domain.order.service;

import com.stocklab.core.domain.order.inbox.OrderEventInbox;
import com.stocklab.core.domain.order.inbox.repository.OrderEventInboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderEventInboxService {

    private final OrderEventInboxRepository orderEventInboxRepository;

    @Transactional(readOnly = true)
    public Optional<OrderEventInbox> findById(Long orderId) {
        return orderEventInboxRepository.findById(orderId);
    }

    @Transactional
    public OrderEventInbox registerProcessing(Long orderId) {
        try {
            return orderEventInboxRepository.save(OrderEventInbox.processing(orderId));
        } catch (DataIntegrityViolationException ex) {
            return null;
        }
    }

    @Transactional
    public void markCompleted(Long orderId) {
        orderEventInboxRepository.findById(orderId).ifPresent(inbox -> {
            inbox.markCompleted();
            orderEventInboxRepository.save(inbox);
        });
    }
}
