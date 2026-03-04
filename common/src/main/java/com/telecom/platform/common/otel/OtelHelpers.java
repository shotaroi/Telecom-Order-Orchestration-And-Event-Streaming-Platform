package com.telecom.platform.common.otel;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

/**
 * OpenTelemetry helper utilities for adding custom attributes to spans.
 */
public final class OtelHelpers {

    private OtelHelpers() {}

    public static void addOrderId(String orderId) {
        Span span = Span.current();
        if (span != null && span.isRecording()) {
            span.setAttribute("order.id", orderId);
        }
    }

    public static void addCustomerId(String customerId) {
        Span span = Span.current();
        if (span != null && span.isRecording()) {
            span.setAttribute("customer.id", customerId);
        }
    }

    public static void addEventId(String eventId) {
        Span span = Span.current();
        if (span != null && span.isRecording()) {
            span.setAttribute("event.id", eventId);
        }
    }
}
