# Telecom Order Orchestration Platform - Repo Structure

```
telecom-order-platform/
├── pom.xml                          # Parent POM
├── docker-compose.yml
├── README.md
├── .gitignore
│
├── common/                          # Shared module
│   └── pom.xml
│   └── src/main/java/com/telecom/platform/common/
│       ├── error/
│       │   ├── ApiError.java
│       │   ├── ErrorCode.java
│       │   └── GlobalExceptionHandler.java
│       ├── api/
│       │   └── ApiResponse.java
│       ├── event/
│       │   ├── OrderCreatedEvent.java
│       │   ├── OrderValidatedEvent.java
│       │   ├── OrderFulfillmentRequestedEvent.java
│       │   ├── ProvisioningCompletedEvent.java
│       │   ├── ProvisioningFailedEvent.java
│       │   ├── OrderFulfilledEvent.java
│       │   └── OrderFailedEvent.java
│       ├── config/
│       │   └── JacksonConfig.java
│       ├── filter/
│       │   └── CorrelationIdFilter.java
│       ├── security/
│       │   └── SecurityUtils.java
│       ├── otel/
│       │   └── OtelHelpers.java
│       └── constants/
│           └── KafkaTopics.java
│
├── order-api/                       # REST API for orders
│   └── pom.xml
│   └── src/main/
│       ├── java/com/telecom/platform/orderapi/
│       │   ├── OrderApiApplication.java
│       │   ├── controller/
│       │   │   └── OrderController.java
│       │   ├── service/
│       │   │   ├── OrderService.java
│       │   │   └── OutboxPublisherService.java
│       │   ├── repository/
│       │   │   ├── OrderRepository.java
│       │   │   ├── OrderStatusHistoryRepository.java
│       │   │   ├── IdempotencyKeyRepository.java
│       │   │   └── OutboxEventRepository.java
│       │   ├── domain/
│       │   │   ├── Order.java
│       │   │   ├── OrderStatus.java
│       │   │   ├── OrderType.java
│       │   │   ├── OrderStatusHistory.java
│       │   │   ├── IdempotencyKey.java
│       │   │   └── OutboxEvent.java
│       │   └── dto/
│       │       ├── CreateOrderRequest.java
│       │       └── OrderResponse.java
│       └── resources/
│           ├── application.yml
│           └── db/migration/
│               ├── V1__create_orders.sql
│               ├── V2__create_order_status_history.sql
│               ├── V3__create_idempotency_keys.sql
│               └── V4__create_outbox_events.sql
│
├── orchestrator-service/
│   └── pom.xml
│   └── src/main/
│       ├── java/com/telecom/platform/orchestrator/
│       │   ├── OrchestratorApplication.java
│       │   ├── consumer/
│       │   │   └── OrderCreatedConsumer.java
│       │   ├── producer/
│       │   │   └── OrchestratorEventProducer.java
│       │   ├── service/
│       │   │   └── OrchestratorService.java
│       │   ├── repository/
│       │   │   └── ProcessedEventRepository.java
│       │   └── domain/
│       │       └── ProcessedEvent.java
│       └── resources/
│           ├── application.yml
│           └── db/migration/
│               └── V1__create_processed_events.sql
│
├── provisioning-service/
│   └── pom.xml
│   └── src/main/
│       ├── java/com/telecom/platform/provisioning/
│       │   ├── ProvisioningApplication.java
│       │   ├── consumer/
│       │   │   └── FulfillmentRequestedConsumer.java
│       │   ├── producer/
│       │   │   └── ProvisioningEventProducer.java
│       │   ├── service/
│       │   │   ├── ProvisioningService.java
│       │   │   └── MockCarrierApiClient.java
│       │   └── repository/
│       │       └── ProvisioningAttemptRepository.java
│       │   └── domain/
│       │       └── ProvisioningAttempt.java
│       └── resources/
│           ├── application.yml
│           └── db/migration/
│               └── V1__create_provisioning_attempts.sql
│
├── support-search-service/
│   └── pom.xml
│   └── src/main/
│       ├── java/com/telecom/platform/support/
│       │   ├── SupportSearchApplication.java
│       │   ├── consumer/
│       │   │   └── OrderEventConsumer.java
│       │   ├── controller/
│       │   │   └── SupportSearchController.java
│       │   ├── service/
│       │   │   └── OrderTimelineService.java
│       │   └── domain/
│       │       └── OrderTimeline.java
│       └── resources/
│           └── application.yml
│
├── gateway-nginx/
│   └── nginx.conf
│
└── observability/
    ├── prometheus/
    │   └── prometheus.yml
    ├── grafana/
    │   └── dashboards/
    │       └── telecom-platform.json
    └── grafana/
        └── provisioning/
            └── alerts.yml
```
