package com.stocklab.core.api;

import com.stocklab.core.api.dto.OrderRequest;
import com.stocklab.core.api.dto.OrderResponse;
import com.stocklab.core.api.security.AuthenticatedUserId;
import com.stocklab.core.domain.order.service.OrderQueryService;
import com.stocklab.core.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    @PostMapping
    public ResponseEntity<Long> placeOrder(
            @AuthenticatedUserId Long userId,
            @Valid @RequestBody OrderRequest request
    ) {
        Long orderId = orderService.placeOrder(userId, request);
        return ResponseEntity.ok(orderId);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticatedUserId Long userId) {
        return ResponseEntity.ok(orderQueryService.getOrdersByUser(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticatedUserId Long userId,
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderQueryService.getOrder(orderId, userId));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @AuthenticatedUserId Long userId,
            @PathVariable Long orderId
    ) {
        orderService.cancelOrder(orderId, userId);
        return ResponseEntity.noContent().build();
    }
}
