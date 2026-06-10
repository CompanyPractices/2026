create table acquirer_fees (
    transmissionDateTime varchar(50) primary key,
    stan varchar(50) not null,
    terminal_id varchar(50) not null,
    acquirer_fee bigint  not null,
    unique (transmissionDateTime, stan, terminal_id)
)
