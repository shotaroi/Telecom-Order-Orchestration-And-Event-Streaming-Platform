package com.telecom.platform.common.event;

import java.time.Instant;
import java.util.UUID;

public record ProvisioningFailedEvent(
        String eventId,
        UUID orderId,
        String msisdn,
        String reason,
        Instant failedAt) {}
