package com.mip.importjob.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.dictionary.entity.FailureMode;
import com.mip.machine.entity.Machine;
import com.mip.record.entity.MaintenanceRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** One parsed row of an import file, carrying its normalisation and resolution results. */
@Entity
@Table(name = "staged_rows", indexes = @Index(name = "ix_staged_job_status", columnList = "job_id, status"))
@Getter
@Setter
@NoArgsConstructor
public class StagedRow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob job;

    @Column(nullable = false)
    private int rowNumber;

    /** Raw parsed columns as a JSON object (header → cell text). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawData;

    // --- normalisation output ---

    @Column(length = 200)
    private String machineText;

    private LocalDate parsedDate;

    private Integer downtimeMinutes;

    @Column(length = 2000)
    private String description;

    @Column(length = 2000)
    private String actionTaken;

    @Column(length = 100)
    private String technician;

    @Column(length = 200)
    private String failureModeText;

    @Column(length = 500)
    private String partsText;

    // --- resolution / extraction output ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    private Double machineConfidence;

    @Column(length = 15)
    private String resolutionMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failure_mode_id")
    private FailureMode failureMode;

    private Double failureModeConfidence;

    /** Combined pipeline confidence in [0,1]. */
    private Double confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StagedRowStatus status = StagedRowStatus.PENDING;

    /** Comma-separated machine-readable reason codes explaining routing. */
    @Column(length = 300)
    private String reasons;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resulting_record_id")
    private MaintenanceRecord resultingRecord;

    public StagedRow(ImportJob job, int rowNumber, String rawData) {
        this.job = job;
        this.rowNumber = rowNumber;
        this.rawData = rawData;
    }
}
