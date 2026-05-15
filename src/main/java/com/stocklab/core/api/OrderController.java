package com.stocklab.core.api;

import com.stocklab.core.api.dto.OrderRequest;
import com.stocklab.core.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Long> placeOrder(@RequestBody OrderRequest request) {
        Long orderId = orderService.placeOrder(request);
        return ResponseEntity.ok(orderId);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long userId
    ) {
        orderService.cancelOrder(orderId, userId);
        return ResponseEntity.noContent().build();
    }
}
