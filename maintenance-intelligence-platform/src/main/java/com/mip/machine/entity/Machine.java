package com.mip.machine.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.plant.entity.Plant;
import com.mip.plant.entity.ProductionLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "machines",
        uniqueConstraints = @UniqueConstraint(name = "uk_machine_plant_code", columnNames = {"plant_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class Machine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id")
    private ProductionLine line;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String manufacturer;

    @Column(length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Criticality criticality = Criticality.MEDIUM;

    private LocalDate commissionedOn;

    @Column(nullable = false)
    private boolean active = true;

    public Machine(Plant plant, ProductionLine line, String code, String name, Criticality criticality) {
        this.plant = plant;
        this.line = line;
        this.code = code;
        this.name = name;
        this.criticality = criticality;
    }
}
