package com.mip.validation.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.importjob.entity.StagedRow;
import com.mip.plant.entity.Plant;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A staged row awaiting a human decision, with the reasons it was flagged. */
@Entity
@Table(name = "validation_items",
        indexes = @Index(name = "ix_validation_plant_status", columnList = "plant_id, status"))
@Getter
@Setter
@NoArgsConstructor
public class ValidationItem extends BaseEntity {

    public enum ValidationStatus {PENDING, APPROVED, REJECTED}

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staged_row_id", nullable = false, unique = true)
    private StagedRow stagedRow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ValidationStatus status = ValidationStatus.PENDING;

    /** Comma-separated reason codes (LOW_CONFIDENCE, UNRESOLVED_MACHINE, ...). */
    @Column(nullable = false, length = 300)
    private String reasons;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private User decidedBy;

    private Instant decidedAt;

    @Column(length = 500)
    private String decisionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resulting_record_id")
    private MaintenanceRecord resultingRecord;

    public ValidationItem(StagedRow stagedRow, Plant plant, String reasons) {
        this.stagedRow = stagedRow;
        this.plant = plant;
        this.reasons = reasons;
    }
}
