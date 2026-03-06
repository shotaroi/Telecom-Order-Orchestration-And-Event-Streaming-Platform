package com.telecom.platform.provisioning.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock Carrier/HLR API for service virtualization.
 * Simulates failures via config for testing retries/DLQ.
 */
@Component
public class MockCarrierApiClient {

    private static final Logger log = LoggerFactory.getLogger(MockCarrierApiClient.class);


    @Value("${app.provisioning.simulate-failure:false}")
    private boolean simulateFailure;

    @Value("${app.provisioning.simulate-timeout:false}")
    private boolean simulateTimeout;

    @Value("${app.provisioning.simulate-5xx:false}")
    private boolean simulate5xx;

    public ProvisionResult provision(UUID orderId, String msisdn) {
        if (simulateTimeout) {
            log.warn("Simulating timeout for orderId={}", orderId);
            throw new RuntimeException("Carrier API timeout");
        }
        if (simulate5xx) {
            log.warn("Simulating 5xx for orderId={}", orderId);
            throw new RuntimeException("Carrier API 503 Service Unavailable");
        }
        if (simulateFailure) {
            log.warn("Simulating failure for orderId={}", orderId);
            return new ProvisionResult(false, "Simulated carrier rejection");
        }
        log.info("Mock provision success: orderId={}, msisdn={}", orderId, msisdn);
        return new ProvisionResult(true, null);
    }

    public record ProvisionResult(boolean success, String errorMessage) {}
}
