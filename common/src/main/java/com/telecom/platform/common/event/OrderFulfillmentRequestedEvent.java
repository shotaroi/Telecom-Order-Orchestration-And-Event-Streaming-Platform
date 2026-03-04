package com.telecom.platform.common.event;

import java.time.Instant;
import java.util.UUID;

public record OrderFulfillmentRequestedEvent(
        String eventId,
        UUID orderId,
        String orderType,
        String customerId,
        String msisdn,
        Instant requestedAt) {}
