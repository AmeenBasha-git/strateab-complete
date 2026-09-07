package com.quantplatform.core.backtest.repository;

import com.quantplatform.core.backtest.domain.BacktestDataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BacktestDatasetRepository extends JpaRepository<BacktestDataset, UUID> {

    List<BacktestDataset> findByUploadedByOrderByCreatedAtDesc(UUID uploadedBy);
}
