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
@Check(constraints = "cash_balance >= 0 AND frozen_balance >= 0")
public class Portfolio {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal cashBalance;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal frozenBalance;

    @Builder
    public Portfolio(User user, BigDecimal cashBalance) {
        this.user = user;
        this.cashBalance = cashBalance != null ? cashBalance : BigDecimal.ZERO;
        this.frozenBalance = BigDecimal.ZERO;
    }

    public BigDecimal getAvailableCash() {
        return cashBalance;
    }

    public void freezeCash(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Freeze amount must be positive");
        }
        if (cashBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient cash balance");
        }
        this.cashBalance = this.cashBalance.subtract(amount);
        this.frozenBalance = this.frozenBalance.add(amount);
    }

    /**
     * Deducts a buy-order reservation from frozen cash and refunds any amount reserved above the actual fill price.
     */
    public void settleBuyReservation(BigDecimal reservedAmount, BigDecimal actualCost) {
        if (reservedAmount.compareTo(BigDecimal.ZERO) <= 0 || actualCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Settlement amounts must be positive");
        }
        if (frozenBalance.compareTo(reservedAmount) < 0) {
            throw new IllegalStateException("Insufficient frozen cash for settlement");
        }

        this.frozenBalance = this.frozenBalance.subtract(reservedAmount);

        if (actualCost.compareTo(reservedAmount) > 0) {
            BigDecimal extraCost = actualCost.subtract(reservedAmount);
            if (cashBalance.compareTo(extraCost) < 0) {
                // In a real system, this might lead to a margin call or overdraft
                throw new IllegalStateException("Insufficient cash balance for price slippage");
            }
            this.cashBalance = this.cashBalance.subtract(extraCost);
        } else {
            BigDecimal refund = reservedAmount.subtract(actualCost);
            this.cashBalance = this.cashBalance.add(refund);
        }
    }

    public void unfreezeCash(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unfreeze amount must be positive");
        }
        if (frozenBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient frozen cash to unfreeze");
        }
        this.frozenBalance = this.frozenBalance.subtract(amount);
        this.cashBalance = this.cashBalance.add(amount);
    }

    public void receiveSaleProceeds(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Proceeds must be positive");
        }
        this.cashBalance = this.cashBalance.add(amount);
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
