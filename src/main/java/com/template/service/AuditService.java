package com.template.service;

import com.template.entity.BaseAuditEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    /**
     * Get audit information for an entity
     */
    public String getAuditInfo(BaseAuditEntity entity) {
        return String.format(
            "Created by: %s on %s, Last updated by: %s on %s, Version: %d",
            entity.getCreatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedBy() != null ? entity.getUpdatedBy() : "N/A",
            entity.getUpdatedAt() != null ? entity.getUpdatedAt() : "N/A",
            entity.getVersion()
        );
    }

    /**
     * Check if entity was created by a specific user
     */
    public boolean isCreatedBy(BaseAuditEntity entity, String username) {
        return username.equals(entity.getCreatedBy());
    }

    /**
     * Check if entity was updated by a specific user
     */
    public boolean isUpdatedBy(BaseAuditEntity entity, String username) {
        return username.equals(entity.getUpdatedBy());
    }

    /**
     * Check if entity was modified within a specific time period
     */
    public boolean isModifiedWithin(BaseAuditEntity entity, LocalDateTime since) {
        return entity.getUpdatedAt() != null && entity.getUpdatedAt().isAfter(since);
    }

    /**
     * Get entities created by a specific user
     */
    public <T extends BaseAuditEntity> List<T> filterByCreator(List<T> entities, String username) {
        return entities.stream()
                .filter(entity -> isCreatedBy(entity, username))
                .toList();
    }

    /**
     * Get entities modified by a specific user
     */
    public <T extends BaseAuditEntity> List<T> filterByModifier(List<T> entities, String username) {
        return entities.stream()
                .filter(entity -> isUpdatedBy(entity, username))
                .toList();
    }

    /**
     * Get entities modified within a time period
     */
    public <T extends BaseAuditEntity> List<T> filterByModificationTime(List<T> entities, LocalDateTime since) {
        return entities.stream()
                .filter(entity -> isModifiedWithin(entity, since))
                .toList();
    }
} 