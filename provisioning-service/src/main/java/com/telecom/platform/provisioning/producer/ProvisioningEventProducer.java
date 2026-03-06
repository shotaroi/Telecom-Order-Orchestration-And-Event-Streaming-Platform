package com.telecom.platform.provisioning.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.platform.common.constants.KafkaTopics;
import com.telecom.platform.common.event.ProvisioningCompletedEvent;
import com.telecom.platform.common.event.ProvisioningFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ProvisioningEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ProvisioningEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ProvisioningEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendCompleted(ProvisioningCompletedEvent event) {
        send(KafkaTopics.PROVISIONING_COMPLETED, event.orderId().toString(), event);
    }

    public void sendFailed(ProvisioningFailedEvent event) {
        send(KafkaTopics.PROVISIONING_FAILED, event.orderId().toString(), event);
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
