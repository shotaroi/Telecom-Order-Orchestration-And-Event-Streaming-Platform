package com.telecom.platform.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.platform.common.event.OrderCreatedEvent;
import com.telecom.platform.common.event.*;
import com.telecom.platform.orchestrator.domain.ProcessedEvent;
import com.telecom.platform.orchestrator.producer.OrchestratorEventProducer;
import com.telecom.platform.orchestrator.repository.ProcessedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);

    private final ObjectMapper objectMapper;
    private final OrchestratorEventProducer producer;
    private final ProcessedEventRepository processedEventRepository;
    private final MeterRegistry meterRegistry;

    private Counter ordersValidatedCounter;
    private Counter ordersFailedCounter;

    public OrchestratorService(ObjectMapper objectMapper, OrchestratorEventProducer producer,
                               ProcessedEventRepository processedEventRepository, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.producer = producer;
        this.processedEventRepository = processedEventRepository;
        this.meterRegistry = meterRegistry;
    }

    @jakarta.annotation.PostConstruct
    void initMetrics() {
        ordersValidatedCounter = meterRegistry.counter("orchestrator_orders_validated_total");
        ordersFailedCounter = meterRegistry.counter("orchestrator_orders_failed_total");
    }

    @KafkaListener(topics = "order.created", groupId = "orchestrator-service")
    @Transactional
    public void consumeOrderCreated(@Payload String payload,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                    Acknowledgment ack) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);

            if (processedEventRepository.existsByEventId(event.eventId())) {
                log.debug("Duplicate event ignored: {}", event.eventId());
                ack.acknowledge();
                return;
            }

            ProcessedEvent pe = new ProcessedEvent();
            pe.setEventId(event.eventId());
            pe.setProcessedAt(Instant.now());
            processedEventRepository.save(pe);

            // Step 1: Validate (simplified - always pass)
            OrderValidatedEvent validated = new OrderValidatedEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    event.orderType(),
                    event.customerId(),
                    event.msisdn(),
                    Instant.now()
            );
            producer.sendOrderValidated(validated);
            ordersValidatedCounter.increment();

            // Step 2: Request fulfillment (provisioning)
            OrderFulfillmentRequestedEvent fulfillmentRequested = new OrderFulfillmentRequestedEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    event.orderType(),
                    event.customerId(),
                    event.msisdn(),
                    Instant.now()
            );
            producer.sendOrderFulfillmentRequested(fulfillmentRequested);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order.created: {}", payload, e);
            ordersFailedCounter.increment();
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "provisioning.completed", groupId = "orchestrator-service")
    @Transactional
    public void consumeProvisioningCompleted(@Payload String payload,
                                             @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                             Acknowledgment ack) {
        try {
            ProvisioningCompletedEvent event = objectMapper.readValue(payload, ProvisioningCompletedEvent.class);

            if (processedEventRepository.existsByEventId(event.eventId())) {
                log.debug("Duplicate event ignored: {}", event.eventId());
                ack.acknowledge();
                return;
            }

            ProcessedEvent pe = new ProcessedEvent();
            pe.setEventId(event.eventId());
            pe.setProcessedAt(Instant.now());
            processedEventRepository.save(pe);

            OrderFulfilledEvent fulfilled = new OrderFulfilledEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    event.msisdn(),
                    Instant.now()
            );
            producer.sendOrderFulfilled(fulfilled);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing provisioning.completed: {}", payload, e);
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "provisioning.failed", groupId = "orchestrator-service")
    @Transactional
    public void consumeProvisioningFailed(@Payload String payload,
                                          @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                          Acknowledgment ack) {
        try {
            ProvisioningFailedEvent event = objectMapper.readValue(payload, ProvisioningFailedEvent.class);

            if (processedEventRepository.existsByEventId(event.eventId())) {
                log.debug("Duplicate event ignored: {}", event.eventId());
                ack.acknowledge();
                return;
            }

            ProcessedEvent pe = new ProcessedEvent();
            pe.setEventId(event.eventId());
            pe.setProcessedAt(Instant.now());
            processedEventRepository.save(pe);

            OrderFailedEvent failed = new OrderFailedEvent(
                    UUID.randomUUID().toString(),
                    event.orderId(),
                    event.reason(),
                    Instant.now()
            );
            producer.sendOrderFailed(failed);
            ordersFailedCounter.increment();
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing provisioning.failed: {}", payload, e);
            throw new RuntimeException(e);
        }
    }
}
