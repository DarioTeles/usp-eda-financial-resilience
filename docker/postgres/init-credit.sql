-- DDL e Carga Inicial para o microsserviço de Crédito (credit_db)
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

CREATE INDEX IF NOT EXISTS idx_credit_outbox_status ON outbox_events(status, created_at);

-- Conta de destino padrão com saldo inicial de R$ 0,00 para a Invariante Financeira
INSERT INTO accounts (id, account_number, balance, updated_at)
VALUES ('b0000000-0000-0000-0000-000000000002', 'ACC-TARGET-002', 0.00, NOW())
ON CONFLICT (id) DO UPDATE SET balance = 0.00, updated_at = NOW();
