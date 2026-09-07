package com.quantplatform.core.strategy.repository;

import com.quantplatform.core.strategy.domain.StrategyVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StrategyVersionRepository extends JpaRepository<StrategyVersion, UUID> {

    List<StrategyVersion> findByStrategyIdOrderByVersionNumberDesc(UUID strategyId);

    Optional<StrategyVersion> findTopByStrategyIdOrderByVersionNumberDesc(UUID strategyId);

    Optional<StrategyVersion> findByStrategyIdAndVersionNumber(UUID strategyId, int versionNumber);
}
