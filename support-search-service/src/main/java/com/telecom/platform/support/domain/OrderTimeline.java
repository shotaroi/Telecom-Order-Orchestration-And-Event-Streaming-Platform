package com.telecom.platform.support.domain;

import java.time.Instant;
import java.util.List;

public record OrderTimeline(
        String orderId,
        String orderType,
        String customerId,
        String msisdn,
        String status,
        Instant createdAt,
        List<TimelineEvent> events) {

    public record TimelineEvent(String eventType, Instant timestamp, String details) {}
}
