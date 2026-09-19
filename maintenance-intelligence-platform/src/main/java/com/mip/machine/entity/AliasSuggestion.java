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
 * A machine name seen in imports that could not be resolved, accumulated across files
 * so a human can map it once and unblock every occurrence.
 */
@Entity
@Table(name = "alias_suggestions",
        uniqueConstraints = @UniqueConstraint(name = "uk_suggestion_plant_text",
                columnNames = {"plant_id", "rawText"}))
@Getter
@Setter
@NoArgsConstructor
public class AliasSuggestion extends BaseEntity {

    public enum SuggestionStatus {PENDING, MAPPED, DISMISSED}

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plant plant;

    /** Normalised unresolved machine text as it appeared in files. */
    @Column(nullable = false, length = 150)
    private String rawText;

    /** Best fuzzy candidate, when one exists below the resolution threshold. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggested_machine_id")
    private Machine suggestedMachine;

    private Double confidence;

    @Column(nullable = false)
    private int occurrences = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    public AliasSuggestion(Plant plant, String rawText, Machine suggestedMachine, Double confidence) {
        this.plant = plant;
        this.rawText = rawText;
        this.suggestedMachine = suggestedMachine;
        this.confidence = confidence;
    }
}
