CREATE TABLE IF NOT EXISTS backtest_datasets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    timeframe VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_bars INT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS backtest_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dataset_id UUID NOT NULL,
    strategy_name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    starting_equity NUMERIC(19,4) NOT NULL,
    final_equity NUMERIC(19,4),
    total_trades INT,
    win_rate NUMERIC(5,2),
    profit_factor NUMERIC(10,4),
    total_pnl NUMERIC(19,4),
    max_drawdown NUMERIC(10,4),
    error_message VARCHAR(2000),
    run_by UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS trade_records (
    id BIGSERIAL PRIMARY KEY,
    strategy_name VARCHAR(255) NOT NULL,
    symbol VARCHAR(255) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    entry_price NUMERIC(19,4) NOT NULL,
    exit_price NUMERIC(19,4) NOT NULL,
    pnl NUMERIC(19,4) NOT NULL,
    execution_time TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS historical_bars (
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
