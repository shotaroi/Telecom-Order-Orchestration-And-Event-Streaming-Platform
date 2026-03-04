package com.telecom.platform.common.event;

import java.time.Instant;
import java.util.UUID;

public record OrderValidatedEvent(
        String eventId,
        UUID orderId,
        String orderType,
        String customerId,
        String msisdn,
        Instant validatedAt) {}
