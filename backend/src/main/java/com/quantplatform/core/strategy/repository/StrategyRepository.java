package com.quantplatform.core.strategy.repository;

import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.domain.StrategyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StrategyRepository extends JpaRepository<Strategy, UUID> {

    @Query("""
           SELECT s FROM Strategy s
           WHERE s.deleted = false
             AND (
                  s.visibility = 'PUBLIC'
                  OR s.strategyType = 'PLATFORM'
                  OR s.ownerId = :requesterId
             )
             AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:strategyType IS NULL OR s.strategyType = :strategyType)
           """)
    Page<Strategy> findVisibleTo(UUID requesterId, String search, StrategyType strategyType, Pageable pageable);

    long countByOwnerIdAndDeletedFalse(UUID ownerId);

    Optional<Strategy> findByNameAndOwnerIdAndDeletedFalse(String name, UUID ownerId);

    List<Strategy> findByOwnerIdAndDeletedFalse(UUID ownerId);
}
