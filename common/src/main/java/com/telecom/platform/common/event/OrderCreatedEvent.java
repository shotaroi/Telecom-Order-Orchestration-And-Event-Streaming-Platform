package com.telecom.platform.common.event;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        String eventId,
        UUID orderId,
        String orderType,
        String customerId,
        String msisdn,
        String status,
        Instant createdAt) {}
