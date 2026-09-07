package com.quantplatform.core.strategy.service;

import com.quantplatform.core.common.exception.ResourceNotFoundException;
import com.quantplatform.core.strategy.domain.StrategyVersion;
import com.quantplatform.core.strategy.repository.StrategyVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StrategyVersionService {

    private final StrategyVersionRepository versionRepository;

    public StrategyVersionService(StrategyVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Transactional
    public StrategyVersion createVersion(
            UUID strategyId,
            String sourceCode,
            String parametersSchema,
            String changelogNote,
            UUID createdBy
    ) {
        int nextVersionNumber = versionRepository.findTopByStrategyIdOrderByVersionNumberDesc(strategyId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        StrategyVersion version = new StrategyVersion(
                strategyId, nextVersionNumber, sourceCode, parametersSchema, changelogNote, createdBy
        );
        return versionRepository.save(version);
    }

    public List<StrategyVersion> listVersions(UUID strategyId) {
        return versionRepository.findByStrategyIdOrderByVersionNumberDesc(strategyId);
    }

    public StrategyVersion getByVersionNumber(UUID strategyId, int versionNumber) {
        return versionRepository.findByStrategyIdAndVersionNumber(strategyId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Version " + versionNumber + " not found for strategy " + strategyId));
    }

    public StrategyVersion getById(UUID versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Strategy version not found: " + versionId));
    }
}
