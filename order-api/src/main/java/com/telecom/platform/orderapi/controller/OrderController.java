package com.telecom.platform.orderapi.controller;

import com.telecom.platform.common.api.ApiResponse;
import com.telecom.platform.common.filter.CorrelationIdFilter;
import com.telecom.platform.common.security.SecurityUtils;
import com.telecom.platform.orderapi.dto.CreateOrderRequest;
import com.telecom.platform.orderapi.dto.OrderResponse;
import com.telecom.platform.orderapi.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN') or isAnonymous()")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = CorrelationIdFilter.CORRELATION_ID_HEADER, required = false) String correlationId,
            @RequestHeader(value = "X-Customer-Id", required = false) String xCustomerId,
            @AuthenticationPrincipal Jwt jwt) {
        String customerId = SecurityUtils.getCustomerId()
                .orElse(xCustomerId != null ? xCustomerId : (jwt != null ? jwt.getSubject() : "dev-customer-1"));
        OrderResponse order = orderService.createOrder(request, customerId, idempotencyKey);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, correlationId));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN') or isAnonymous()")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable UUID orderId,
            @RequestHeader(value = CorrelationIdFilter.CORRELATION_ID_HEADER, required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        String customerId = SecurityUtils.getCustomerId().orElse(jwt != null ? jwt.getSubject() : null);
        OrderResponse order;
        if (SecurityUtils.hasRole("ADMIN")) {
            order = orderService.getOrderAdmin(orderId);
        } else {
            order = orderService.getOrder(orderId, customerId);
        }
        return ResponseEntity.ok(ApiResponse.success(order, correlationId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN') or isAnonymous()")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestParam(required = false) String customerId,
            @RequestHeader(value = CorrelationIdFilter.CORRELATION_ID_HEADER, required = false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        String effectiveCustomerId = customerId != null ? customerId
                : SecurityUtils.getCustomerId().orElse(jwt != null ? jwt.getSubject() : null);
        if (effectiveCustomerId == null) {
            return ResponseEntity.badRequest().build();
        }
        if (customerId != null && !SecurityUtils.hasRole("ADMIN")) {
            effectiveCustomerId = SecurityUtils.getCustomerId().orElse(effectiveCustomerId);
        }
        List<OrderResponse> orders = orderService.getOrdersByCustomer(effectiveCustomerId);
        return ResponseEntity.ok(ApiResponse.success(orders, correlationId));
    }
}
