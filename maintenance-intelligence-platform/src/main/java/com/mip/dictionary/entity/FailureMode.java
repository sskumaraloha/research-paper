package com.mip.dictionary.entity;

import com.mip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "failure_modes")
@Getter
@Setter
@NoArgsConstructor
public class FailureMode extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FailureCategory category;

    /** Lower-case, comma-separated trigger words used by the failure-mode extractor. */
    @Column(length = 500)
    private String keywords;

    public FailureMode(String code, String name, FailureCategory category, String keywords) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.keywords = keywords;
    }

    public List<String> keywordList() {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
