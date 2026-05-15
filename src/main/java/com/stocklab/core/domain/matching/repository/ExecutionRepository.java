package com.stocklab.core.domain.matching.repository;

import com.stocklab.core.domain.matching.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {
}
