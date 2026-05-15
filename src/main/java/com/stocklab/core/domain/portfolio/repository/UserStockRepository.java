package com.stocklab.core.domain.portfolio.repository;

import com.stocklab.core.domain.portfolio.UserStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserStockRepository extends JpaRepository<UserStock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT us FROM UserStock us WHERE us.user.id = :userId AND us.stock.stockCode = :stockCode")
    Optional<UserStock> findByUserIdAndStockCodeForUpdate(
            @Param("userId") Long userId,
            @Param("stockCode") String stockCode
    );

    Optional<UserStock> findByUser_IdAndStock_StockCode(Long userId, String stockCode);
}
