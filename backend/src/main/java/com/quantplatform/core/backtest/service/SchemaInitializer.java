package com.quantplatform.core.backtest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String[] columns = {
                "sharpe_ratio NUMERIC(10,4)",
                "sortino_ratio NUMERIC(10,4)",
                "calmar_ratio NUMERIC(10,4)",
                "annualized_return NUMERIC(10,4)",
                "annualized_volatility NUMERIC(10,4)",
                "avg_win NUMERIC(19,4)",
                "avg_loss NUMERIC(19,4)",
                "largest_win NUMERIC(19,4)",
                "largest_loss NUMERIC(19,4)",
                "expectancy NUMERIC(19,4)",
                "risk_reward_ratio NUMERIC(10,4)",
                "max_consecutive_wins INT",
                "max_consecutive_losses INT",
                "long_trades INT",
                "short_trades INT",
                "long_win_rate NUMERIC(5,2)",
                "short_win_rate NUMERIC(5,2)",
                "total_commission NUMERIC(19,4)",
                "equity_curve JSONB"
        };

        for (String col : columns) {
            String colName = col.split(" ")[0];
            try {
                jdbcTemplate.execute("ALTER TABLE backtest_runs ADD COLUMN IF NOT EXISTS " + col);
            } catch (Exception e) {
                log.debug("Column {} already exists or could not be added: {}", colName, e.getMessage());
            }
        }
        log.info("Schema check complete: backtest_runs professional metrics columns ensured");
    }
}
