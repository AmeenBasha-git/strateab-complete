package com.quantplatform.core.strategy.service;

import com.quantplatform.core.common.exception.ForbiddenException;
import com.quantplatform.core.strategy.domain.Strategy;
import com.quantplatform.core.strategy.domain.StrategyVisibility;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Centralizes read/write permission checks for strategies so both the
 * strategy and version controllers apply identical rules instead of each
 * re-implementing ownership/visibility logic.
 */
@Service
public class StrategyAuthorizationService {

    public boolean canRead(Strategy strategy, UUID requesterId, Set<String> requesterRoles) {
        if (requesterRoles.contains("ADMIN")) return true;
        if (strategy.isPlatformStrategy()) return true;
        if (strategy.getVisibility() == StrategyVisibility.PUBLIC) return true;
        if (strategy.isOwnedBy(requesterId)) return true;
        return strategy.getVisibility() == StrategyVisibility.SHARED;
    }

    public boolean canWrite(Strategy strategy, UUID requesterId, Set<String> requesterRoles) {
        if (requesterRoles.contains("ADMIN")) return true;
        if (strategy.isPlatformStrategy()) return false; // only admins write platform strategies
        return strategy.isOwnedBy(requesterId);
    }

    public void assertCanRead(Strategy strategy, UUID requesterId, Set<String> requesterRoles) {
        if (!canRead(strategy, requesterId, requesterRoles)) {
            throw new ForbiddenException("You do not have access to this strategy");
        }
    }

    public void assertCanWrite(Strategy strategy, UUID requesterId, Set<String> requesterRoles) {
        if (!canWrite(strategy, requesterId, requesterRoles)) {
            throw new ForbiddenException("You do not have permission to modify this strategy");
        }
    }
}
