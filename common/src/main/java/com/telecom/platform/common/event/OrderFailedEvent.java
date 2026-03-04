package com.telecom.platform.common.event;

import java.time.Instant;
import java.util.UUID;

public record OrderFailedEvent(
        String eventId,
        UUID orderId,
        String reason,
        Instant failedAt) {}
