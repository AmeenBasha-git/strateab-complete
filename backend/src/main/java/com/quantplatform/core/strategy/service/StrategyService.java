package com.quantplatform.core.strategy.service;

import com.quantplatform.core.audit.service.AuditLogService;
import com.quantplatform.core.common.exception.ResourceNotFoundException;
import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.domain.StrategyType;
import com.quantplatform.core.strategy.domain.StrategyVersion;
import com.quantplatform.core.strategy.domain.StrategyVisibility;
import com.quantplatform.core.strategy.repository.StrategyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Orchestrates strategy creation and metadata management. Version creation
 * itself is delegated to {@link StrategyVersionService} — this class only
 * decides *when* a version gets created (e.g. always on initial strategy
 * creation) and manages which version is "active".
 */
@Service
public class StrategyService {

    private final StrategyRepository strategyRepository;
    private final StrategyVersionService strategyVersionService;
    private final AuditLogService auditLogService;

    public StrategyService(
            StrategyRepository strategyRepository,
            StrategyVersionService strategyVersionService,
            AuditLogService auditLogService
    ) {
        this.strategyRepository = strategyRepository;
        this.strategyVersionService = strategyVersionService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Strategy createUserStrategy(UUID ownerId, String name, String description, String sourceCode, String parametersSchema) {
        Strategy strategy = Strategy.createUserStrategy(ownerId, name, description);
        strategyRepository.save(strategy);

        StrategyVersion initialVersion = strategyVersionService.createVersion(
                strategy.getId(), sourceCode, parametersSchema, "Initial version", ownerId
        );
        strategy.setActiveVersion(initialVersion.getId());
        strategyRepository.save(strategy);

        auditLogService.record(ownerId, "STRATEGY_CREATED", "Strategy", strategy.getId().toString());
        return strategy;
    }

    @Transactional
    public Strategy createPlatformStrategy(UUID adminId, String name, String description, String sourceCode, String parametersSchema) {
        Strategy strategy = Strategy.createPlatformStrategy(name, description);
        strategyRepository.save(strategy);

        StrategyVersion initialVersion = strategyVersionService.createVersion(
                strategy.getId(), sourceCode, parametersSchema, "Initial version", adminId
        );
        strategy.setActiveVersion(initialVersion.getId());
        strategyRepository.save(strategy);

        auditLogService.record(adminId, "PLATFORM_STRATEGY_CREATED", "Strategy", strategy.getId().toString());
        return strategy;
    }

    public Strategy getById(UUID id) {
        Strategy strategy = strategyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Strategy not found: " + id));
        if (strategy.isDeleted()) {
            throw new ResourceNotFoundException("Strategy not found: " + id);
        }
        return strategy;
    }

    public Page<Strategy> search(UUID requesterId, String query, StrategyType filterType, Pageable pageable) {
        return strategyRepository.findVisibleTo(requesterId, query, filterType, pageable);
    }

    @Transactional
    public Strategy updateMetadata(UUID id, String name, String description, StrategyVisibility visibility, UUID actingUserId) {
        Strategy strategy = getById(id);
        strategy.updateMetadata(name, description, visibility);
        strategyRepository.save(strategy);
        auditLogService.record(actingUserId, "STRATEGY_UPDATED", "Strategy", id.toString());
        return strategy;
    }

    @Transactional
    public void softDelete(UUID id, UUID actingUserId) {
        Strategy strategy = getById(id);
        strategy.markDeleted();
        strategyRepository.save(strategy);
        auditLogService.record(actingUserId, "STRATEGY_DELETED", "Strategy", id.toString());
    }

    @Transactional
    public Strategy setActiveVersion(UUID strategyId, UUID versionId, UUID actingUserId) {
        Strategy strategy = getById(strategyId);
        strategyVersionService.getById(versionId);
        strategy.setActiveVersion(versionId);
        strategyRepository.save(strategy);
        auditLogService.record(actingUserId, "STRATEGY_ACTIVE_VERSION_CHANGED", "Strategy", strategyId.toString());
        return strategy;
    }
}
