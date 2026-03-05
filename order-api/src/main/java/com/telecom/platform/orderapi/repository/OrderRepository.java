package com.telecom.platform.orderapi.repository;

import com.telecom.platform.orderapi.domain.Order;
import com.telecom.platform.orderapi.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    Optional<Order> findByIdAndCustomerId(UUID id, String customerId);
}
