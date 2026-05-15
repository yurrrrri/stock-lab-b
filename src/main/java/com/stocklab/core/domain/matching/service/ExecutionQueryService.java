package com.stocklab.core.domain.matching.service;

import com.stocklab.core.api.dto.ExecutionResponse;
import com.stocklab.core.domain.matching.repository.ExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExecutionQueryService {

    private final ExecutionRepository executionRepository;

    public List<ExecutionResponse> getExecutionsByUser(Long userId) {
        return executionRepository.findByUserInvolved(userId).stream()
                .map(ExecutionResponse::from)
                .toList();
    }

    public List<ExecutionResponse> getExecutionsByStock(String stockCode) {
        return executionRepository.findByStockCode(stockCode).stream()
                .map(ExecutionResponse::from)
                .toList();
    }
}
