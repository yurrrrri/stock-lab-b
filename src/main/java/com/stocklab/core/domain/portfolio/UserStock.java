package com.stocklab.core.domain.portfolio;

import com.stocklab.core.domain.auth.User;
import com.stocklab.core.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(
    name = "user_stocks",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "stock_code"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Check(constraints = "quantity >= 0")
public class UserStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userStockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_code", nullable = false)
    private Stock stock;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal averagePrice;

    @Builder
    public UserStock(User user, Stock stock, BigDecimal quantity, BigDecimal averagePrice) {
        this.user = user;
        this.stock = stock;
        this.quantity = quantity != null ? quantity : BigDecimal.ZERO;
        this.averagePrice = averagePrice != null ? averagePrice : BigDecimal.ZERO;
    }
    
    public void addQuantity(BigDecimal amount, BigDecimal price) {
        BigDecimal totalValue = this.quantity.multiply(this.averagePrice).add(amount.multiply(price));
        this.quantity = this.quantity.add(amount);
        if (this.quantity.compareTo(BigDecimal.ZERO) > 0) {
            this.averagePrice = totalValue.divide(this.quantity, 4, java.math.RoundingMode.HALF_UP);
        }
    }
    
    public void subtractQuantity(BigDecimal amount) {
        if (this.quantity.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient stock quantity");
        }
        this.quantity = this.quantity.subtract(amount);
    }
}
