package com.telecom.platform.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Consistent API response wrapper for success responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, String correlationId) {

    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(data, correlationId);
    }
}
