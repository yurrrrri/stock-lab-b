package com.stocklab.core.domain.matching;

import com.stocklab.core.domain.auth.User;
import com.stocklab.core.domain.order.Order;
import com.stocklab.core.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "executions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long executionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_code", nullable = false)
    private Stock stock;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal executionPrice;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal executionQuantity;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime executedAt;

    @Builder
    public Execution(Order order, User buyer, User seller, Stock stock, 
                     BigDecimal executionPrice, BigDecimal executionQuantity) {
        this.order = order;
        this.buyer = buyer;
        this.seller = seller;
        this.stock = stock;
        this.executionPrice = executionPrice;
        this.executionQuantity = executionQuantity;
    }
}
