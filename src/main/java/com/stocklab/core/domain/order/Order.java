package com.stocklab.core.domain.order;

import com.stocklab.core.domain.BaseTimeEntity;
import com.stocklab.core.domain.auth.User;
import com.stocklab.core.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_created", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_orders_stock_status", columnList = "stock_code, status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_code", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide orderSide;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal filledQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Builder
    public Order(User user, Stock stock, OrderType orderType, OrderSide orderSide, 
                 BigDecimal price, BigDecimal quantity) {
        this.user = user;
        this.stock = stock;
        this.orderType = orderType;
        this.orderSide = orderSide;
        this.price = price;
        this.quantity = quantity;
        this.filledQuantity = BigDecimal.ZERO;
        this.status = OrderStatus.PENDING;
    }

    public void fill(BigDecimal fillQuantity) {
        this.filledQuantity = this.filledQuantity.add(fillQuantity);
        if (this.filledQuantity.compareTo(this.quantity) >= 0) {
            this.status = OrderStatus.COMPLETED;
        } else {
            this.status = OrderStatus.PARTIAL;
        }
    }

    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order");
        }
        this.status = OrderStatus.CANCELED;
    }
}
