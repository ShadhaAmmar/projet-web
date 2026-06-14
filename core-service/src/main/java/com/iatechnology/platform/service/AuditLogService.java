package com.iatechnology.platform.service;

import com.iatechnology.platform.entity.AuditLog;
import com.iatechnology.platform.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void logAction(String action, String entityName, String username) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entityName(entityName)
                .username(username)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }
}
