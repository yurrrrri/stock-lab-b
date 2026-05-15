package com.stocklab.core.domain.matching.repository;

import com.stocklab.core.domain.matching.MatchIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchIdempotencyRepository extends JpaRepository<MatchIdempotencyRecord, String> {
}
