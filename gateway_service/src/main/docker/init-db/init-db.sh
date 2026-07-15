#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Gateway Service
    -- CREATE USER gateway_service;
    -- CREATE DATABASE gateway_service OWNER gateway_service;
    -- GRANT ALL PRIVILEGES ON DATABASE gateway_service TO gateway_service;

    -- Order Service
    CREATE USER order_service;
    CREATE DATABASE order_service OWNER order_service;
    GRANT ALL PRIVILEGES ON DATABASE order_service TO order_service;

    -- Payment Service
    CREATE USER payment_service;
    CREATE DATABASE payment_service OWNER payment_service;
    GRANT ALL PRIVILEGES ON DATABASE payment_service TO payment_service;

    -- Inventory Service
    CREATE USER inventory_service;
    CREATE DATABASE inventory_service OWNER inventory_service;
    GRANT ALL PRIVILEGES ON DATABASE inventory_service TO inventory_service;
EOSQL