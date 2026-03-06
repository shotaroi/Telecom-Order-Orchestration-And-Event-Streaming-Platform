package com.telecom.platform.support.controller;

import com.telecom.platform.common.api.ApiResponse;
import com.telecom.platform.common.filter.CorrelationIdFilter;
import com.telecom.platform.support.domain.OrderTimeline;
import com.telecom.platform.support.service.OrderTimelineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/support")
public class SupportSearchController {

    private final OrderTimelineService timelineService;

    public SupportSearchController(OrderTimelineService timelineService) {
        this.timelineService = timelineService;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderTimeline>>> search(
            @RequestParam("q") String query,
            @RequestHeader(value = CorrelationIdFilter.CORRELATION_ID_HEADER, required = false) String correlationId) {
        List<OrderTimeline> results = timelineService.search(query);
        return ResponseEntity.ok(ApiResponse.success(results, correlationId));
    }

    @GetMapping("/orders/{orderId}/timeline")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderTimeline>> getTimeline(
            @PathVariable String orderId,
            @RequestHeader(value = CorrelationIdFilter.CORRELATION_ID_HEADER, required = false) String correlationId) {
        return timelineService.getTimeline(orderId)
                .map(t -> ResponseEntity.ok(ApiResponse.success(t, correlationId)))
                .orElse(ResponseEntity.notFound().build());
    }
}
