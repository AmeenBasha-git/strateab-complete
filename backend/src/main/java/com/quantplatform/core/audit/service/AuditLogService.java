package com.quantplatform.core.audit.service;

import com.quantplatform.core.audit.domain.AuditLog;
import com.quantplatform.core.audit.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Writes audit trail entries asynchronously so audit logging never adds
 * latency to the primary request path (login, role change, etc.).
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void record(UUID actorUserId, String action, String entityType, String entityId) {
        AuditLog log = new AuditLog(actorUserId, action, entityType, entityId);
        auditLogRepository.save(log);
    }
}
