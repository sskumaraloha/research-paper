package com.mip.insight.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.record.entity.MaintenanceRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "insight_evidence")
@Getter
@Setter
@NoArgsConstructor
public class InsightEvidence extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insight_id", nullable = false)
    private Insight insight;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private MaintenanceRecord record;

    @Column(length = 300)
    private String note;

    public InsightEvidence(Insight insight, MaintenanceRecord record, String note) {
        this.insight = insight;
        this.record = record;
        this.note = note;
    }
}
