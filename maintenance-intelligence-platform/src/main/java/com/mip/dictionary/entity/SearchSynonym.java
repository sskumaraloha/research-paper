package com.mip.dictionary.entity;

import com.mip.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Maps a colloquial or abbreviated term to its canonical form within a domain. */
@Entity
@Table(name = "search_synonyms",
        uniqueConstraints = @UniqueConstraint(name = "uk_synonym_domain_term", columnNames = {"domain", "term"}))
@Getter
@Setter
@NoArgsConstructor
public class SearchSynonym extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SynonymDomain domain;

    /** Lower-case surface form as users type or files spell it. */
    @Column(nullable = false, length = 100)
    private String term;

    /** Lower-case canonical form the term expands to. */
    @Column(nullable = false, length = 100)
    private String canonicalTerm;

    public SearchSynonym(SynonymDomain domain, String term, String canonicalTerm) {
        this.domain = domain;
        this.term = term;
        this.canonicalTerm = canonicalTerm;
    }
}
