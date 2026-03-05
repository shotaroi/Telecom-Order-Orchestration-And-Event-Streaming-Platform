package com.telecom.platform.orderapi.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.platform.common.event.OrderFailedEvent;
import com.telecom.platform.common.event.OrderFulfilledEvent;
import com.telecom.platform.orderapi.domain.OrderStatus;
import com.telecom.platform.orderapi.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderStatusEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public OrderStatusEventConsumer(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = "order.fulfilled", groupId = "order-api-status")
    @Transactional
    public void consumeOrderFulfilled(@Payload String payload,
                                      @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                      Acknowledgment ack) {
        try {
            OrderFulfilledEvent event = objectMapper.readValue(payload, OrderFulfilledEvent.class);
            orderService.updateStatus(event.orderId(), OrderStatus.FULFILLED);
            log.info("Order fulfilled: orderId={}", event.orderId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order.fulfilled: {}", payload, e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "order.failed", groupId = "order-api-status")
    @Transactional
    public void consumeOrderFailed(@Payload String payload,
                                   @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                   Acknowledgment ack) {
        try {
            OrderFailedEvent event = objectMapper.readValue(payload, OrderFailedEvent.class);
            orderService.updateStatus(event.orderId(), OrderStatus.FAILED);
            log.info("Order failed: orderId={}, reason={}", event.orderId(), event.reason());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order.failed: {}", payload, e);
            throw new RuntimeException(e);
        }
    }
}
