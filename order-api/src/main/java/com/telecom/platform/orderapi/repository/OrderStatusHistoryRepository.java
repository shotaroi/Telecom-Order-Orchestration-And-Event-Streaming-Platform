package com.telecom.platform.orderapi.repository;

import com.telecom.platform.orderapi.domain.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
