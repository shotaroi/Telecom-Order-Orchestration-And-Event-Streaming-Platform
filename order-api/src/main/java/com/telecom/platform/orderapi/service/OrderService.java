package com.telecom.platform.orderapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.platform.common.constants.KafkaTopics;
import com.telecom.platform.common.event.OrderCreatedEvent;
import com.telecom.platform.common.error.ResourceNotFoundException;
import com.telecom.platform.orderapi.domain.*;
import com.telecom.platform.orderapi.dto.CreateOrderRequest;
import com.telecom.platform.orderapi.dto.OrderResponse;
import com.telecom.platform.orderapi.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private Counter ordersCreatedCounter;

    public OrderService(OrderRepository orderRepository, OrderStatusHistoryRepository statusHistoryRepository,
                        IdempotencyKeyRepository idempotencyKeyRepository, OutboxEventRepository outboxEventRepository,
                        ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @jakarta.annotation.PostConstruct
    void initMetrics() {
        ordersCreatedCounter = meterRegistry.counter("orders_created_total");
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String customerId, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyKeyRepository.findById(idempotencyKey)
                    .map(ik -> {
                        Order order = orderRepository.findById(ik.getOrderId())
                                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
                        return toResponse(order);
                    })
                    .orElseGet(() -> doCreateOrder(request, customerId, idempotencyKey));
        }
        return doCreateOrder(request, customerId, null);
    }

    private OrderResponse doCreateOrder(CreateOrderRequest request, String customerId, String idempotencyKey) {
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();

        Order order = new Order();
        order.setId(orderId);
        order.setType(request.orderType());
        order.setCustomerId(customerId);
        order.setMsisdn(request.msisdn());
        order.setStatus(OrderStatus.CREATED);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrderId(orderId);
        history.setFromStatus(null);
        history.setToStatus(OrderStatus.CREATED.name());
        history.setCreatedAt(now);
        statusHistoryRepository.save(history);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey ik = new IdempotencyKey();
            ik.setIdempotencyKey(idempotencyKey);
            ik.setOrderId(orderId);
            ik.setCreatedAt(now);
            idempotencyKeyRepository.save(ik);
        }

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                orderId,
                request.orderType().name(),
                customerId,
                request.msisdn(),
                OrderStatus.CREATED.name(),
                now);

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outbox = new OutboxEvent();
            outbox.setAggregateId(orderId);
            outbox.setEventType(KafkaTopics.ORDER_CREATED);
            outbox.setPayloadJson(payload);
            outbox.setStatus("NEW");
            outbox.setCreatedAt(now);
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }

        ordersCreatedCounter.increment();
        log.info("Order created: orderId={}, customerId={}, type={}", orderId, customerId, request.orderType());
        return toResponse(order);
    }

    public OrderResponse getOrder(UUID orderId, String customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    public OrderResponse getOrderAdmin(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(orderId);
        h.setFromStatus(oldStatus.name());
        h.setToStatus(newStatus.name());
        h.setCreatedAt(Instant.now());
        statusHistoryRepository.save(h);
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.StatusHistoryItem> history = statusHistoryRepository
                .findByOrderIdOrderByCreatedAtAsc(order.getId()).stream()
                .map(h -> new OrderResponse.StatusHistoryItem(
                        h.getFromStatus(),
                        h.getToStatus(),
                        h.getCreatedAt()))
                .collect(Collectors.toList());
        return OrderResponse.from(order, history);
    }
}
