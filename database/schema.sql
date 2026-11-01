CREATE DATABASE IF NOT EXISTS renewable_energy;
USE renewable_energy;

-- Spring Boot JPA creates/updates these tables on startup.
-- Table outline for reference:
CREATE TABLE IF NOT EXISTS energy_sources (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  type VARCHAR(30) NOT NULL DEFAULT 'Unspecified',
  capacity_kwh DOUBLE NOT NULL
);
CREATE TABLE IF NOT EXISTS consumer_loads (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(30) NOT NULL,
  demand_kwh DOUBLE NOT NULL,
  minimum_service_kwh DOUBLE NOT NULL,
  divisible BOOLEAN NOT NULL,
  priority INT NOT NULL,
  request_order INT NOT NULL
);
CREATE TABLE IF NOT EXISTS allocation_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  interval_number INT NOT NULL,
  load_id BIGINT,
  load_name VARCHAR(120),
  category VARCHAR(30),
  requested_kwh DOUBLE NOT NULL,
  allocated_kwh DOUBLE NOT NULL,
  unmet_kwh DOUBLE NOT NULL,
  curtailed_kwh DOUBLE NOT NULL,
  generation_kwh DOUBLE NOT NULL,
  created_at DATETIME
);
