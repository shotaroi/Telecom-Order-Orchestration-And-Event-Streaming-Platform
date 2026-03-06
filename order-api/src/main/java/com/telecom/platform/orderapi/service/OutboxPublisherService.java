package com.telecom.platform.orderapi.service;

import com.telecom.platform.common.constants.KafkaTopics;
import com.telecom.platform.orderapi.domain.OutboxEvent;
import com.telecom.platform.orderapi.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publishOutboxEvents() {
        var events = outboxEventRepository.findNewEventsForPublishing(PageRequest.of(0, 50));
        for (OutboxEvent event : events) {
            try {
                String topic = resolveTopic(event.getEventType());
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayloadJson()).get();
                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}", event.getId(), e);
            }
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case KafkaTopics.ORDER_CREATED -> KafkaTopics.ORDER_CREATED;
            default -> eventType;
        };
    }
}
