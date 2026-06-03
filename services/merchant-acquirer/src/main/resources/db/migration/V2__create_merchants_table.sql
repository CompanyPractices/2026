create table merchants (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    mcc CHAR(4) NOT NULL,
    category VARCHAR(50) NOT NULL,
    acquirer_id VARCHAR(50) NOT NULL,
    acquiring_fee NUMERIC(5, 4) NOT NULL,
    average_check BIGINT NOT NULL
);