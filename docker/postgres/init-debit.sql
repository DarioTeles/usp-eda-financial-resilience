-- DDL e Carga Inicial para o microsserviço de Débito (debit_db)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    balance NUMERIC(15, 2) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_debit_outbox_status ON outbox_events(status, created_at);

-- Conta de origem padrão com saldo inicial de R$ 10.000,00 para a Invariante Financeira
INSERT INTO accounts (id, account_number, balance, updated_at)
VALUES ('a0000000-0000-0000-0000-000000000001', 'ACC-ORIGIN-001', 10000.00, NOW())
ON CONFLICT (id) DO UPDATE SET balance = 10000.00, updated_at = NOW();
