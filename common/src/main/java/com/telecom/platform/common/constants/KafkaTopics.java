package com.telecom.platform.common.constants;

/**
 * Kafka topic names. Retry and DLQ topics follow convention: {topic}.retry, {topic}.dlq
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    // Primary topics
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_VALIDATED = "order.validated";
    public static final String ORDER_FULFILLMENT_REQUESTED = "order.fulfillment.requested";
    public static final String PROVISIONING_COMPLETED = "provisioning.completed";
    public static final String PROVISIONING_FAILED = "provisioning.failed";
    public static final String ORDER_FULFILLED = "order.fulfilled";
    public static final String ORDER_FAILED = "order.failed";

    // Retry topics (exponential backoff)
    public static final String ORDER_CREATED_RETRY = "order.created.retry";
    public static final String ORDER_CREATED_DLQ = "order.created.dlq";
    public static final String ORDER_FULFILLMENT_REQUESTED_RETRY = "order.fulfillment.requested.retry";
    public static final String ORDER_FULFILLMENT_REQUESTED_DLQ = "order.fulfillment.requested.dlq";
}
