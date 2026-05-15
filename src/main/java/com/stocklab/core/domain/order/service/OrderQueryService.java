package com.stocklab.core.domain.order.service;

import com.stocklab.core.api.dto.OrderResponse;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public OrderResponse getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByOrderIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return OrderResponse.from(order);
    }

    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

}
