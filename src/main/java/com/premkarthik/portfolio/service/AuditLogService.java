package com.premkarthik.portfolio.service;

import com.premkarthik.portfolio.model.AuditLog;

import java.util.List;

public interface AuditLogService {
    AuditLog log(String action, String entityType, Long entityId, String username, String details);
    List<AuditLog> getTaskAuditLogs();
}
