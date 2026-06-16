create table if not exists acquirer_fee (
    id  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transmission_date_time  VARCHAR(64) NOT NULL,
    stan    VARCHAR(6) NOT NULL,
    pan VARCHAR(19) NOT NULL,
    terminal_id VARCHAR(20) NOT NULL,
    acquirer_fee NUMERIC(19, 0) NOT NULL,
    amount NUMERIC(19, 0) NOT NULL,
    CONSTRAINT uk_acquirer_fee_tx UNIQUE (terminal_id, stan, transmission_date_time)
)
