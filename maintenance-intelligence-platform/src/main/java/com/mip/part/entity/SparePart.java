package com.mip.part.entity;

import com.mip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "spare_parts")
@Getter
@Setter
@NoArgsConstructor
public class SparePart extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String partNumber;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 50)
    private String category;

    public SparePart(String partNumber, String name, String category) {
        this.partNumber = partNumber;
        this.name = name;
        this.category = category;
    }
}
