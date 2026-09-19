package com.mip.plant.entity;

import com.mip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "production_lines",
        uniqueConstraints = @UniqueConstraint(name = "uk_line_plant_code", columnNames = {"plant_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
public class ProductionLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    public ProductionLine(Plant plant, String code, String name) {
        this.plant = plant;
        this.code = code;
        this.name = name;
    }
}
