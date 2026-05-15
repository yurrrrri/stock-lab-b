package com.stocklab.core.domain.portfolio;

import com.stocklab.core.domain.auth.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;

@Entity
@Table(name = "portfolios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Check(constraints = "cash_balance >= 0")
public class Portfolio {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal cashBalance;

    @Builder
    public Portfolio(User user, BigDecimal cashBalance) {
        this.user = user;
        this.cashBalance = cashBalance != null ? cashBalance : BigDecimal.ZERO;
    }
    
    public void deposit(BigDecimal amount) {
        this.cashBalance = this.cashBalance.add(amount);
    }
    
    public void withdraw(BigDecimal amount) {
        if (this.cashBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient cash balance");
        }
        this.cashBalance = this.cashBalance.subtract(amount);
    }
}
