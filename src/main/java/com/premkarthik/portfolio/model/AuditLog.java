package com.premkarthik.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;
    private String action;
    private String entityType;
    private Long entityId;
    private String username;
    private String details;
    private LocalDateTime timestamp;

    public AuditLog(String action, String entityType, Long entityId, String username, String details) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.username = username;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
}
