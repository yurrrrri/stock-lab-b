package com.stocklab.core.domain.order.inbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_event_inbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEventInbox {

    @Id
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderEventInboxStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    public static OrderEventInbox processing(Long orderId) {
        OrderEventInbox inbox = new OrderEventInbox();
        inbox.orderId = orderId;
        inbox.status = OrderEventInboxStatus.PROCESSING;
        inbox.createdAt = LocalDateTime.now();
        return inbox;
    }

    public void markCompleted() {
        this.status = OrderEventInboxStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
