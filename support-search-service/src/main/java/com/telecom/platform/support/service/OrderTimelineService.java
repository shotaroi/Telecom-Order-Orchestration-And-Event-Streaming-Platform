package com.telecom.platform.support.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.platform.common.event.*;
import com.telecom.platform.support.domain.OrderTimeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes order events and indexes OrderTimeline in OpenSearch.
 * For simplicity, also maintains in-memory for demo; production would use OpenSearch as source of truth.
 */
@Service
public class OrderTimelineService {

    private static final Logger log = LoggerFactory.getLogger(OrderTimelineService.class);
    private static final String INDEX = "order-timelines";

    private final ObjectMapper objectMapper;
    private final OpenSearchClient openSearchClient;

    public OrderTimelineService(ObjectMapper objectMapper, OpenSearchClient openSearchClient) {
        this.objectMapper = objectMapper;
        this.openSearchClient = openSearchClient;
    }

    private final Map<String, OrderTimeline> timelineCache = new ConcurrentHashMap<>();

    @KafkaListener(topics = {
            "order.created", "order.validated", "order.fulfillment.requested",
            "provisioning.completed", "provisioning.failed",
            "order.fulfilled", "order.failed"
    }, groupId = "support-search-service")
    public void consumeOrderEvent(@Payload String payload,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                   Acknowledgment ack) {
        try {
            OrderTimeline timeline = buildTimelineFromEvent(topic, payload);
            if (timeline != null) {
                timelineCache.put(timeline.orderId(), timeline);
                indexTimeline(timeline);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing event from {}: {}", topic, payload, e);
            throw new RuntimeException(e);
        }
    }

    private OrderTimeline buildTimelineFromEvent(String topic, String payload) throws Exception {
        return switch (topic) {
            case "order.created" -> {
                OrderCreatedEvent e = objectMapper.readValue(payload, OrderCreatedEvent.class);
                yield new OrderTimeline(
                        e.orderId().toString(),
                        e.orderType(),
                        e.customerId(),
                        e.msisdn(),
                        e.status(),
                        e.createdAt(),
                        List.of(new OrderTimeline.TimelineEvent("order.created", e.createdAt(), "Order created")));
            }
            case "order.validated" -> {
                OrderValidatedEvent e = objectMapper.readValue(payload, OrderValidatedEvent.class);
                yield mergeTimelineEvent(e.orderId().toString(), "order.validated", e.validatedAt(), "Order validated");
            }
            case "order.fulfillment.requested" -> {
                OrderFulfillmentRequestedEvent e = objectMapper.readValue(payload, OrderFulfillmentRequestedEvent.class);
                yield mergeTimelineEvent(e.orderId().toString(), "order.fulfillment.requested", e.requestedAt(), "Provisioning requested");
            }
            case "provisioning.completed" -> {
                ProvisioningCompletedEvent e = objectMapper.readValue(payload, ProvisioningCompletedEvent.class);
                yield mergeTimelineEvent(e.orderId().toString(), "provisioning.completed", e.completedAt(), "Provisioning completed");
            }
            case "provisioning.failed" -> {
                ProvisioningFailedEvent e = objectMapper.readValue(payload, ProvisioningFailedEvent.class);
                yield mergeTimelineEvent(e.orderId().toString(), "provisioning.failed", e.failedAt(), e.reason());
            }
            case "order.fulfilled" -> {
                OrderFulfilledEvent e = objectMapper.readValue(payload, OrderFulfilledEvent.class);
                yield mergeTimelineEvent(e.orderId().toString(), "order.fulfilled", e.fulfilledAt(), "Order fulfilled");
            }
            case "order.failed" -> {
                OrderFailedEvent e = objectMapper.readValue(payload, OrderFailedEvent.class);
                yield mergeTimelineEvent(e.orderId().toString(), "order.failed", e.failedAt(), e.reason());
            }
            default -> null;
        };
    }

    private OrderTimeline mergeTimelineEvent(String orderId, String eventType, Instant timestamp, String details) throws Exception {
        OrderTimeline existing = timelineCache.get(orderId);
        if (existing == null) {
            existing = fetchFromOpenSearch(orderId).orElse(null);
        }
        List<OrderTimeline.TimelineEvent> events = existing != null
                ? new ArrayList<>(existing.events())
                : new ArrayList<>();
        events.add(new OrderTimeline.TimelineEvent(eventType, timestamp, details));
        events.sort(Comparator.comparing(OrderTimeline.TimelineEvent::timestamp));

        String status = "order.fulfilled".equals(eventType) ? "FULFILLED"
                : "order.failed".equals(eventType) ? "FAILED"
                : existing != null ? existing.status() : "PROCESSING";

        return new OrderTimeline(
                orderId,
                existing != null ? existing.orderType() : "",
                existing != null ? existing.customerId() : "",
                existing != null ? existing.msisdn() : "",
                status,
                existing != null ? existing.createdAt() : timestamp,
                events);
    }

    private void indexTimeline(OrderTimeline timeline) {
        try {
            openSearchClient.index(IndexRequest.of(i -> i
                    .index(INDEX)
                    .id(timeline.orderId())
                    .document(timeline)));
        } catch (Exception e) {
            log.error("Failed to index timeline for orderId={}", timeline.orderId(), e);
        }
    }

    public Optional<OrderTimeline> getTimeline(String orderId) {
        OrderTimeline cached = timelineCache.get(orderId);
        if (cached != null) return Optional.of(cached);
        return fetchFromOpenSearch(orderId);
    }

    private Optional<OrderTimeline> fetchFromOpenSearch(String orderId) {
        try {
            GetResponse<OrderTimeline> response = openSearchClient.get(g -> g
                    .index(INDEX)
                    .id(orderId), OrderTimeline.class);
            return response.found() ? Optional.of(response.source()) : Optional.empty();
        } catch (Exception e) {
            log.debug("Timeline not found for orderId={}", orderId);
            return Optional.empty();
        }
    }

    public List<OrderTimeline> search(String query) {
        try {
            SearchResponse<OrderTimeline> response = openSearchClient.search(s -> s
                    .index(INDEX)
                    .query(q -> q
                            .multiMatch(m -> m
                                    .query(query)
                                    .fields("orderId", "customerId", "msisdn", "orderType", "status"))), OrderTimeline.class);
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Search failed: {}", query, e);
            return List.of();
        }
    }
}
