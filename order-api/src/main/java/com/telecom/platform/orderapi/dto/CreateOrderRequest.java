package com.telecom.platform.orderapi.dto;

import com.telecom.platform.orderapi.domain.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateOrderRequest(
        @NotNull(message = "orderType is required")
        OrderType orderType,

        @NotBlank(message = "msisdn is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "msisdn must be a valid phone number")
        String msisdn) {}
