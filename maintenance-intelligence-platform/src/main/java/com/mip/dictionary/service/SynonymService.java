package com.mip.dictionary.service;

import com.mip.dictionary.entity.SearchSynonym;
import com.mip.dictionary.entity.SynonymDomain;
import com.mip.dictionary.repository.SearchSynonymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class SynonymService {

    private final SearchSynonymRepository synonymRepository;

    /** Canonical form of a term within one domain; the term itself when no mapping exists. */
    @Transactional(readOnly = true)
    public String canonicalise(String term, SynonymDomain domain) {
        String needle = term.trim().toLowerCase(Locale.ROOT);
        return synonymRepository.findByDomainAndTerm(domain, needle)
                .map(SearchSynonym::getCanonicalTerm)
                .orElse(needle);
    }

    /** All forms a term may appear as: the term, its canonical form, and every synonym of that canonical form. */
    @Transactional(readOnly = true)
    public Set<String> expandTerm(String term) {
        String needle = term.trim().toLowerCase(Locale.ROOT);
        Set<String> expansions = new LinkedHashSet<>();
        expansions.add(needle);
        for (SearchSynonym direct : synonymRepository.findByTerm(needle)) {
            expansions.add(direct.getCanonicalTerm());
        }
        // reverse direction: needle may itself be a canonical term
        for (SearchSynonym synonym : synonymRepository.findAll()) {
            if (expansions.contains(synonym.getCanonicalTerm())) {
                expansions.add(synonym.getTerm());
            }
        }
        return expansions;
    }

    /** Full dictionary grouped by domain, canonical term → surface forms. */
    @Transactional(readOnly = true)
    public Map<String, Map<String, List<String>>> getDictionary() {
        Map<String, Map<String, List<String>>> dictionary = new TreeMap<>();
        for (SynonymDomain domain : SynonymDomain.values()) {
            Map<String, List<String>> byCanonical = new TreeMap<>();
            for (SearchSynonym synonym : synonymRepository.findByDomain(domain)) {
                byCanonical.computeIfAbsent(synonym.getCanonicalTerm(), k -> new java.util.ArrayList<>())
                        .add(synonym.getTerm());
            }
            dictionary.put(domain.name(), byCanonical);
        }
        return dictionary;
    }
}
