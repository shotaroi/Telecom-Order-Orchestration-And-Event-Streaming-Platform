package com.telecom.platform.orderapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test with Testcontainers (MySQL + Kafka).
 * Run with: mvn -pl order-api test -Dtest=OrderApiIntegrationTest
 */
@SpringBootTest(properties = {"app.security.dev-mode=true", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@AutoConfigureMockMvc
@Testcontainers
class OrderApiIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("telecom_orders")
            .withUsername("telecom")
            .withPassword("telecom")
            ;

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void createOrder_returns201() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Customer-Id", "cust-test")
                        .header("Idempotency-Key", "idem-001")
                        .content("{\"orderType\":\"SIM_ACTIVATION\",\"msisdn\":\"+15551234567\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void idempotency_sameKeyReturnsSameOrder() throws Exception {
        String key = "idem-" + System.currentTimeMillis();
        var result1 = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Customer-Id", "cust-idem")
                        .header("Idempotency-Key", key)
                        .content("{\"orderType\":\"PLAN_CHANGE\",\"msisdn\":\"+15559999999\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id1 = com.jayway.jsonpath.JsonPath.read(result1.getResponse().getContentAsString(), "$.data.id");

        var result2 = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Customer-Id", "cust-idem")
                        .header("Idempotency-Key", key)
                        .content("{\"orderType\":\"PLAN_CHANGE\",\"msisdn\":\"+15559999999\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id2 = com.jayway.jsonpath.JsonPath.read(result2.getResponse().getContentAsString(), "$.data.id");

        org.junit.jupiter.api.Assertions.assertEquals(id1, id2, "Idempotency: same key should return same order id");
    }
}
