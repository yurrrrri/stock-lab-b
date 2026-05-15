package com.stocklab.core.domain.order.inbox.repository;

import com.stocklab.core.domain.order.inbox.OrderEventInbox;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderEventInboxRepository extends JpaRepository<OrderEventInbox, Long> {
}
