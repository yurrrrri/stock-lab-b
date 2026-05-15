package com.stocklab.core.domain.matching.repository;

import com.stocklab.core.domain.matching.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    @Query("""
            SELECT e FROM Execution e
            JOIN FETCH e.order
            JOIN FETCH e.buyer
            JOIN FETCH e.seller
            JOIN FETCH e.stock
            WHERE e.buyer.id = :userId OR e.seller.id = :userId
            ORDER BY e.executedAt DESC
            """)
    List<Execution> findByUserInvolved(@Param("userId") Long userId);

    @Query("""
            SELECT e FROM Execution e
            JOIN FETCH e.order
            JOIN FETCH e.buyer
            JOIN FETCH e.seller
            JOIN FETCH e.stock
            WHERE e.stock.stockCode = :stockCode
            ORDER BY e.executedAt DESC
            """)
    List<Execution> findByStockCode(@Param("stockCode") String stockCode);
}
