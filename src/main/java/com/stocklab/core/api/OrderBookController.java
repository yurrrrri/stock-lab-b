package com.stocklab.core.api;

import com.stocklab.core.api.dto.ExecutionResponse;
import com.stocklab.core.api.dto.OrderBookResponse;
import com.stocklab.core.domain.matching.service.ExecutionQueryService;
import com.stocklab.core.domain.matching.service.OrderBookQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orderbook")
@RequiredArgsConstructor
public class OrderBookController {

    private final OrderBookQueryService orderBookQueryService;
    private final ExecutionQueryService executionQueryService;

    @GetMapping("/{stockCode}")
    public ResponseEntity<OrderBookResponse> getOrderBook(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "10") int depth
    ) {
        return ResponseEntity.ok(orderBookQueryService.getOrderBook(stockCode, depth));
    }

    @GetMapping("/{stockCode}/executions")
    public ResponseEntity<List<ExecutionResponse>> getStockExecutions(@PathVariable String stockCode) {
        return ResponseEntity.ok(executionQueryService.getExecutionsByStock(stockCode));
    }
}
