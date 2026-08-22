package com.premkarthik.portfolio.service;

import com.premkarthik.portfolio.model.AuditLog;
import com.premkarthik.portfolio.repository.AuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "audit.log.enabled", havingValue = "true")
public class MongoAuditLogService implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public MongoAuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLog log(String action, String entityType, Long entityId, String username, String details) {
        return auditLogRepository.save(new AuditLog(action, entityType, entityId, username, details));
    }

    @Override
    public List<AuditLog> getTaskAuditLogs() {
        return auditLogRepository.findByEntityTypeOrderByTimestampDesc("Task");
    }
}
