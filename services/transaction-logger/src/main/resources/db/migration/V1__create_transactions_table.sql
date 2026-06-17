CREATE TABLE transactions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    mti VARCHAR(4),
    stan VARCHAR(6) NOT NULL,
    rrn VARCHAR(12),
    pan VARCHAR(16) NOT NULL,
    processing_code VARCHAR(6) NOT NULL,
    amount NUMERIC NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    terminal_id VARCHAR(8) NOT NULL,
    terminal_type VARCHAR(10),
    merchant_id VARCHAR(15) NOT NULL,
    mcc VARCHAR(4) NOT NULL,
    acquirer_id VARCHAR(10) NOT NULL,
    issuer_id VARCHAR(10),
    acquiring_fee NUMERIC,
    status VARCHAR(20) NOT NULL,
    decline_reason VARCHAR(100),
    auth_code VARCHAR(6),
    processing_time_ms INTEGER,
    transmission_date_time TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_status ON transactions (status);
CREATE INDEX idx_transactions_created_at ON transactions (created_at);
CREATE INDEX idx_transactions_pan ON transactions (pan);
CREATE INDEX idx_transactions_merchant ON transactions (merchant_id);
