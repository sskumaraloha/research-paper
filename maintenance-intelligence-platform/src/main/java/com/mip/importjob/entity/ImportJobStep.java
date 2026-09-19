package com.mip.importjob.entity;

import com.mip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "import_job_steps")
@Getter
@Setter
@NoArgsConstructor
public class ImportJobStep extends BaseEntity {

    public enum StepStatus {PENDING, RUNNING, COMPLETED, FAILED}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ImportStepName name;

    @Column(nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StepStatus status = StepStatus.PENDING;

    @Column(nullable = false)
    private int processedCount;

    @Column(length = 1000)
    private String message;

    private Instant startedAt;

    private Instant finishedAt;

    public ImportJobStep(ImportJob job, ImportStepName name, int orderIndex) {
        this.job = job;
        this.name = name;
        this.orderIndex = orderIndex;
    }
}
