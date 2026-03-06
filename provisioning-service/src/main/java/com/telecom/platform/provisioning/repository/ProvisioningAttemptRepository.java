package com.telecom.platform.provisioning.repository;

import com.telecom.platform.provisioning.domain.ProvisioningAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProvisioningAttemptRepository extends JpaRepository<ProvisioningAttempt, Long> {

    List<ProvisioningAttempt> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
