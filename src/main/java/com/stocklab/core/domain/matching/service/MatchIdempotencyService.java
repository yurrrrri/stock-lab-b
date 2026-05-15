package com.stocklab.core.domain.matching.service;

import com.stocklab.core.domain.matching.MatchIdempotencyRecord;
import com.stocklab.core.domain.matching.repository.MatchIdempotencyRepository;
import com.stocklab.core.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class MatchIdempotencyService {

    private final MatchIdempotencyRepository matchIdempotencyRepository;

    @Transactional
    public boolean tryAcquire(Order buyOrder, Order sellOrder) {
        String matchKey = buildMatchKey(buyOrder, sellOrder);
        try {
            matchIdempotencyRepository.save(
                    MatchIdempotencyRecord.create(matchKey, buyOrder.getOrderId(), sellOrder.getOrderId())
            );
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    private String buildMatchKey(Order buyOrder, Order sellOrder) {
        return buyOrder.getOrderId()
                + ":" + sellOrder.getOrderId()
                + ":" + formatQuantity(buyOrder.getFilledQuantity())
                + ":" + formatQuantity(sellOrder.getFilledQuantity());
    }

    private String formatQuantity(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }
}
