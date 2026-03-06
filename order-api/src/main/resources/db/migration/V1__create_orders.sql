-- Orders table: RDS-ready schema with appropriate indexes
CREATE TABLE orders (
    id BINARY(16) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    msisdn VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for customer lookup (support: "get my orders")
CREATE INDEX idx_orders_customer_id ON orders(customer_id);

-- Index for MSISDN lookup (support search, number port-in checks)
CREATE INDEX idx_orders_msisdn ON orders(msisdn);

-- Index for status filtering
CREATE INDEX idx_orders_status ON orders(status);
