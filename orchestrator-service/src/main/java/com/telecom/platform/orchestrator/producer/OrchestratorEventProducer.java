package com.telecom.platform.orchestrator.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.platform.common.constants.KafkaTopics;
import com.telecom.platform.common.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrchestratorEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrchestratorEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOrderValidated(OrderValidatedEvent event) {
        send(KafkaTopics.ORDER_VALIDATED, event.orderId().toString(), event);
    }

    public void sendOrderFulfillmentRequested(OrderFulfillmentRequestedEvent event) {
        send(KafkaTopics.ORDER_FULFILLMENT_REQUESTED, event.orderId().toString(), event);
    }

    public void sendOrderFulfilled(OrderFulfilledEvent event) {
        send(KafkaTopics.ORDER_FULFILLED, event.orderId().toString(), event);
    }

    public void sendOrderFailed(OrderFailedEvent event) {
        send(KafkaTopics.ORDER_FAILED, event.orderId().toString(), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload);
            log.debug("Sent event to {}: {}", topic, key);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
