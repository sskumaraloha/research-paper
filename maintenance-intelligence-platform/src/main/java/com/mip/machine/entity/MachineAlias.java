package com.mip.machine.entity;

import com.mip.common.entity.BaseEntity;
import com.mip.plant.entity.Plant;
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

/**
 * A name a machine is known by in import files or on the floor. The alias is stored
 * normalised (lower-case, collapsed whitespace) and is unique within its plant so
 * that resolution is deterministic.
 */
@Entity
@Table(name = "machine_aliases",
        uniqueConstraints = @UniqueConstraint(name = "uk_alias_plant_alias", columnNames = {"plant_id", "alias"}))
@Getter
@Setter
@NoArgsConstructor
public class MachineAlias extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    /** Denormalised from the machine for the plant-scoped unique constraint and lookups. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    @Column(nullable = false, length = 150)
    private String alias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AliasSource source;

    public MachineAlias(Machine machine, String alias, AliasSource source) {
        this.machine = machine;
        this.plant = machine.getPlant();
        this.alias = alias;
        this.source = source;
    }
}
