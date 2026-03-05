package com.telecom.platform.orderapi.dto;

import com.telecom.platform.orderapi.domain.Order;
import com.telecom.platform.orderapi.domain.OrderStatus;
import com.telecom.platform.orderapi.domain.OrderType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderType type,
        String customerId,
        String msisdn,
        OrderStatus status,
        Instant createdAt,
        List<StatusHistoryItem> statusHistory) {

    public record StatusHistoryItem(String fromStatus, String toStatus, Instant createdAt) {}

    public static OrderResponse from(Order order, List<StatusHistoryItem> history) {
        return new OrderResponse(
                order.getId(),
                order.getType(),
                order.getCustomerId(),
                order.getMsisdn(),
                order.getStatus(),
                order.getCreatedAt(),
                history != null ? history : List.of());
    }
}
