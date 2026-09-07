CREATE TABLE historical_bars (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    timestamp_et TIMESTAMPTZ NOT NULL,
    open NUMERIC(18,6) NOT NULL,
    high NUMERIC(18,6) NOT NULL,
    low NUMERIC(18,6) NOT NULL,
    close NUMERIC(18,6) NOT NULL,
    volume NUMERIC(18,2) NOT NULL,
    vwap_rth NUMERIC(18,6),
    vwap_eth NUMERIC(18,6),
    CONSTRAINT uq_symbol_timestamp UNIQUE (symbol, timestamp_et)
);
