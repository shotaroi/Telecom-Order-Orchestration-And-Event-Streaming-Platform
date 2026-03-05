package com.telecom.platform.orchestrator.config;

import com.telecom.platform.common.constants.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderValidatedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_VALIDATED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderFulfillmentRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_FULFILLMENT_REQUESTED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderFulfilledTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_FULFILLED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic orderFailedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_FAILED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic provisioningCompletedTopic() {
        return TopicBuilder.name(KafkaTopics.PROVISIONING_COMPLETED).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic provisioningFailedTopic() {
        return TopicBuilder.name(KafkaTopics.PROVISIONING_FAILED).partitions(3).replicas(1).build();
    }
}
