package com.mip.plant.entity;

import com.mip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** The company a plant belongs to; purely organisational grouping. */
@Entity
@Table(name = "organisations")
@Getter
@Setter
@NoArgsConstructor
public class Organisation extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    public Organisation(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
