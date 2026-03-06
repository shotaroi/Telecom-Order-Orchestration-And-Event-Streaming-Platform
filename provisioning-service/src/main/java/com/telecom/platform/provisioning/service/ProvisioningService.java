package com.telecom.platform.provisioning.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.platform.common.event.OrderFulfillmentRequestedEvent;
import com.telecom.platform.common.event.ProvisioningCompletedEvent;
import com.telecom.platform.common.event.ProvisioningFailedEvent;
import com.telecom.platform.provisioning.domain.ProvisioningAttempt;
import com.telecom.platform.provisioning.producer.ProvisioningEventProducer;
import com.telecom.platform.provisioning.repository.ProvisioningAttemptRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
public class ProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(ProvisioningService.class);

    private final ObjectMapper objectMapper;
    private final MockCarrierApiClient mockCarrierApi;
    private final ProvisioningEventProducer producer;
    private final ProvisioningAttemptRepository attemptRepository;
    private final MeterRegistry meterRegistry;

    private Counter provisioningCompletedCounter;
    private Counter provisioningFailedCounter;
    private Timer provisioningDurationTimer;

    public ProvisioningService(ObjectMapper objectMapper, MockCarrierApiClient mockCarrierApi,
                               ProvisioningEventProducer producer, ProvisioningAttemptRepository attemptRepository,
                               MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.mockCarrierApi = mockCarrierApi;
        this.producer = producer;
        this.attemptRepository = attemptRepository;
        this.meterRegistry = meterRegistry;
    }

    @jakarta.annotation.PostConstruct
    void initMetrics() {
        provisioningCompletedCounter = meterRegistry.counter("provisioning_completed_total");
        provisioningFailedCounter = meterRegistry.counter("provisioning_failed_total");
        provisioningDurationTimer = meterRegistry.timer("provisioning_duration_seconds");
    }

    @KafkaListener(topics = "order.fulfillment.requested", groupId = "provisioning-service")
    @Transactional
    public void consumeFulfillmentRequested(@Payload String payload,
                                            @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                            Acknowledgment ack) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            OrderFulfillmentRequestedEvent event = objectMapper.readValue(payload, OrderFulfillmentRequestedEvent.class);

            var result = mockCarrierApi.provision(event.orderId(), event.msisdn());

            ProvisioningAttempt attempt = new ProvisioningAttempt();
            attempt.setOrderId(event.orderId());
            attempt.setMsisdn(event.msisdn());
            attempt.setStatus(result.success() ? "COMPLETED" : "FAILED");
            attempt.setAttemptNumber(1);
            attempt.setErrorMessage(result.errorMessage());
            attempt.setCreatedAt(Instant.now());
            attemptRepository.save(attempt);

            if (result.success()) {
                producer.sendCompleted(new ProvisioningCompletedEvent(
                        UUID.randomUUID().toString(),
                        event.orderId(),
                        event.msisdn(),
                        Instant.now()
                ));
                provisioningCompletedCounter.increment();
            } else {
                producer.sendFailed(new ProvisioningFailedEvent(
                        UUID.randomUUID().toString(),
                        event.orderId(),
                        event.msisdn(),
                        result.errorMessage(),
                        Instant.now()
                ));
                provisioningFailedCounter.increment();
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Error processing order.fulfillment.requested: {}", payload, e);
            provisioningFailedCounter.increment();
            throw new RuntimeException(e);
        } finally {
            sample.stop(provisioningDurationTimer);
        }
    }
}
