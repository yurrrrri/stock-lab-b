package com.stocklab.core.domain.outbox.repository;

import com.stocklab.core.domain.outbox.OutboxMessage;
import com.stocklab.core.domain.outbox.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM OutboxMessage o
            WHERE o.status = :status
            ORDER BY o.id ASC
            """)
    List<OutboxMessage> findPendingForDispatch(@Param("status") OutboxStatus status, Pageable pageable);
}
