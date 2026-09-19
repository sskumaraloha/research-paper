package com.mip.plant.entity;

import com.mip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plants")
@Getter
@Setter
@NoArgsConstructor
public class Plant extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String location;

    // --- plant settings (import pipeline and alerting thresholds) ---

    /** Staged rows scoring at or above this confidence are imported without human review. */
    @Column(nullable = false)
    private double autoApproveThreshold = 0.85;

    /** Staged rows scoring below this confidence are rejected outright. */
    @Column(nullable = false)
    private double lowConfidenceThreshold = 0.40;

    /** A single downtime event at or above this many minutes is considered alert-worthy. */
    @Column(nullable = false)
    private int downtimeAlertMinutes = 240;

    public Plant(String code, String name, String location) {
        this.code = code;
        this.name = name;
        this.location = location;
    }
}
