package com.stocklab.core.domain.order.repository;

import com.stocklab.core.domain.order.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

    @Query("SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.stock WHERE o.orderId = :orderId AND o.user.id = :userId")
    Optional<Order> findByOrderIdAndUser_Id(@Param("orderId") Long orderId, @Param("userId") Long userId);

    @Query("SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.stock WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUser_IdOrderByCreatedAtDesc(@Param("userId") Long userId);

}
