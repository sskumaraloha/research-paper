package com.mip.audit.entity;

import com.mip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable trail of security- and data-relevant actions. Actor and plant are stored
 * as snapshots (id + name), not foreign keys, so the trail survives account changes.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "ix_audit_plant", columnList = "plantId"),
        @Index(name = "ix_audit_actor", columnList = "actorId")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends BaseEntity {

    @Column(nullable = false)
    private Long actorId;

    @Column(nullable = false, length = 100)
    private String actorName;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 30)
    private String entityType;

    private Long entityId;

    private Long plantId;

    @Column(length = 500)
    private String detail;

    public AuditLog(Long actorId, String actorName, String action, String entityType,
                    Long entityId, Long plantId, String detail) {
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.plantId = plantId;
        this.detail = detail;
    }
}
