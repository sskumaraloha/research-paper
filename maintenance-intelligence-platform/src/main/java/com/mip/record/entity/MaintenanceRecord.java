package com.mip.record.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.dictionary.entity.FailureMode;
import com.mip.machine.entity.Machine;
import com.mip.part.entity.SparePart;
import com.mip.plant.entity.Plant;
import com.mip.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "maintenance_records", indexes = {
        @Index(name = "ix_record_plant_date", columnList = "plant_id, recordDate"),
        @Index(name = "ix_record_machine_date", columnList = "machine_id, recordDate")
})
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "failure_mode_id")
    private FailureMode failureMode;

    @Column(nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false)
    private int downtimeMinutes;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 2000)
    private String actionTaken;

    @Column(length = 100)
    private String technician;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private RecordSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecordStatus status = RecordStatus.ACTIVE;

    /** Pipeline confidence for imported rows; null for manual entries. */
    private Double confidence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id")
    private SourceDocument sourceDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(length = 500)
    private String rejectedReason;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "record_spare_parts",
            joinColumns = @JoinColumn(name = "record_id"),
            inverseJoinColumns = @JoinColumn(name = "part_id"))
    private Set<SparePart> spareParts = new HashSet<>();
}
