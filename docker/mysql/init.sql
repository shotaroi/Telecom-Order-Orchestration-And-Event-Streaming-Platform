CREATE DATABASE IF NOT EXISTS telecom_orders;
CREATE DATABASE IF NOT EXISTS telecom_orchestrator;
CREATE DATABASE IF NOT EXISTS telecom_provisioning;
GRANT ALL PRIVILEGES ON telecom_orders.* TO 'telecom'@'%';
GRANT ALL PRIVILEGES ON telecom_orchestrator.* TO 'telecom'@'%';
GRANT ALL PRIVILEGES ON telecom_provisioning.* TO 'telecom'@'%';
FLUSH PRIVILEGES;
