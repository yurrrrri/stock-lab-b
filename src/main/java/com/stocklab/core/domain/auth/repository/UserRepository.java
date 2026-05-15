package com.stocklab.core.domain.auth.repository;

import com.stocklab.core.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
