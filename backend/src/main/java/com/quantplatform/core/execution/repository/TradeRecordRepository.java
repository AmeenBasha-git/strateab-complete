package com.quantplatform.core.execution.repository;

import com.quantplatform.core.execution.model.TradeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRecordRepository extends JpaRepository<TradeRecord, Long> {
    List<TradeRecord> findByStrategyName(String strategyName);
}
