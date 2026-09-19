package com.mip.importjob.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.plant.entity.Plant;
import com.mip.record.entity.SourceDocument;
import com.mip.user.entity.User;
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
@Table(name = "import_jobs")
@Getter
@Setter
@NoArgsConstructor
public class ImportJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_document_id", nullable = false)
    private SourceDocument sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "triggered_by", nullable = false)
    private User triggeredBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ImportJobStatus status = ImportJobStatus.PENDING;

    @Column(nullable = false)
    private int totalRows;

    @Column(nullable = false)
    private int autoImportedCount;

    @Column(nullable = false)
    private int needsValidationCount;

    @Column(nullable = false)
    private int rejectedCount;

    @Column(nullable = false)
    private int invalidCount;

    @Column(length = 1000)
    private String errorMessage;

    private Instant startedAt;

    private Instant finishedAt;

    public ImportJob(Plant plant, SourceDocument sourceDocument, User triggeredBy) {
        this.plant = plant;
        this.sourceDocument = sourceDocument;
        this.triggeredBy = triggeredBy;
    }
}
