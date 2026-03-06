CREATE TABLE provisioning_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BINARY(16) NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_number INT NOT NULL DEFAULT 1,
    error_message TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_provisioning_order_id ON provisioning_attempts(order_id);
