package com.telecom.platform.orderapi.repository;

import com.telecom.platform.orderapi.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {
}
