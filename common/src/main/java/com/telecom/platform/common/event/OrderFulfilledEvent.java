package com.telecom.platform.common.event;

import java.time.Instant;
import java.util.UUID;

public record OrderFulfilledEvent(
        String eventId,
        UUID orderId,
        String msisdn,
        Instant fulfilledAt) {}
