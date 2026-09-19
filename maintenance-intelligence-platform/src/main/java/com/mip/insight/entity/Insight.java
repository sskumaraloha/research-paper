package com.mip.insight.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.machine.entity.Machine;
import com.mip.plant.entity.Plant;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A detected pattern worth a maintenance engineer's attention, with its evidence records. */
@Entity
@Table(name = "insights", indexes = @Index(name = "ix_insight_plant", columnList = "plant_id"))
@Getter
@Setter
@NoArgsConstructor
public class Insight extends BaseEntity {

    public enum Severity {INFO, WARNING, CRITICAL}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    /** Null for plant-level insights. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private InsightType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String detail;

    /** The number the detector fired on (occurrences, ratio, share %...). */
    private Double metricValue;

    @Column(nullable = false)
    private int windowDays;

    @Column(nullable = false)
    private Instant computedAt;

    @OneToMany(mappedBy = "insight", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InsightEvidence> evidence = new ArrayList<>();

    public Insight(Plant plant, Machine machine, InsightType type, Severity severity,
                   String title, String detail, Double metricValue, int windowDays) {
        this.plant = plant;
        this.machine = machine;
        this.type = type;
        this.severity = severity;
        this.title = title;
        this.detail = detail;
        this.metricValue = metricValue;
        this.windowDays = windowDays;
        this.computedAt = Instant.now();
    }
}
