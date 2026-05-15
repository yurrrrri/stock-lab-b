package com.stocklab.core.domain.stock.repository;

import com.stocklab.core.domain.stock.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, String> {
}
