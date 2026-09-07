package com.quantplatform.core.backtest.repository;

import com.quantplatform.core.backtest.domain.BacktestRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BacktestRunRepository extends JpaRepository<BacktestRun, UUID> {

    List<BacktestRun> findByRunByOrderByCreatedAtDesc(UUID runBy);

    List<BacktestRun> findByDatasetIdOrderByCreatedAtDesc(UUID datasetId);

    long countByRunBy(UUID runBy);
}
