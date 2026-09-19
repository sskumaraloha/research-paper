package com.mip.schedule.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.machine.entity.Machine;
import com.mip.plant.entity.Plant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * A recurring preventive-maintenance plan for one machine. Completing it creates a
 * PREVENTIVE maintenance record and rolls the next due date forward by the interval.
 */
@Entity
@Table(name = "maintenance_schedules",
        indexes = @Index(name = "ix_schedule_plant_due", columnList = "plant_id, nextDueOn"))
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private int intervalDays;

    private LocalDate lastPerformedOn;

    @Column(nullable = false)
    private LocalDate nextDueOn;

    @Column(nullable = false)
    private boolean active = true;

    /** Last day an overdue notification went out, so the daily sweep never spams. */
    private LocalDate lastOverdueNotifiedOn;

    public MaintenanceSchedule(Plant plant, Machine machine, String title, String description,
                               int intervalDays, LocalDate nextDueOn) {
        this.plant = plant;
        this.machine = machine;
        this.title = title;
        this.description = description;
        this.intervalDays = intervalDays;
        this.nextDueOn = nextDueOn;
    }
}
