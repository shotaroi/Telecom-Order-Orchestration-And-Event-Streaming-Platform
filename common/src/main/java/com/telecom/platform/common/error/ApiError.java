package com.telecom.platform.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * RFC 7807 Problem+JSON style error response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String correlationId,
        String traceId,
        Instant timestamp,
        List<FieldError> errors) {

    public record FieldError(String field, String message) {}
}
