package com.stocklab.core.domain.order.repository;

import com.stocklab.core.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
