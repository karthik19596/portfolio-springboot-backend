package com.premkarthik.portfolio.service;

import com.premkarthik.portfolio.model.AuditLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "audit.log.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAuditLogService implements AuditLogService {

    @Override
    public AuditLog log(String action, String entityType, Long entityId, String username, String details) {
        return new AuditLog(action, entityType, entityId, username, details);
    }

    @Override
    public List<AuditLog> getTaskAuditLogs() {
        return Collections.emptyList();
    }
}
