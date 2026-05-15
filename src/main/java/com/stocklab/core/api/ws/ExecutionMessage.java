package com.stocklab.core.api.ws;

import com.stocklab.core.domain.matching.Execution;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExecutionMessage(
        Long executionId,
        Long orderId,
        Long buyerId,
        Long sellerId,
        String stockCode,
        BigDecimal executionPrice,
        BigDecimal executionQuantity,
        LocalDateTime executedAt
) {
    public static ExecutionMessage from(Execution execution) {
        return new ExecutionMessage(
                execution.getExecutionId(),
                execution.getOrder().getOrderId(),
                execution.getBuyer().getId(),
                execution.getSeller().getId(),
                execution.getStock().getStockCode(),
                execution.getExecutionPrice(),
                execution.getExecutionQuantity(),
                execution.getExecutedAt()
        );
    }
}
