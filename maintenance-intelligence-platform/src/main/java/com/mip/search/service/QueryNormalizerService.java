package com.mip.search.service;

import com.mip.common.util.TextNormalizer;
import com.mip.dictionary.service.SynonymService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Prepares free text for searching: normalises it, drops stop words, and expands
 * every remaining term through the synonym dictionary so "brg failure" also finds
 * records that say "bearing".
 */
@Service
@RequiredArgsConstructor
public class QueryNormalizerService {

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "of", "on", "in", "for", "to", "and", "or", "is", "are", "was",
            "were", "what", "which", "show", "me", "all", "with", "at", "by", "how", "many",
            "much", "list", "find", "get", "give", "about", "did", "do", "does", "has", "have");

    private final SynonymService synonymService;

    public String normalise(String query) {
        return TextNormalizer.normalize(query);
    }

    /** Meaningful tokens of the query, stop words removed. */
    public List<String> tokens(String query) {
        return Arrays.stream(normalise(query).split(" "))
                .filter(token -> !token.isBlank() && !STOP_WORDS.contains(token))
                .toList();
    }

    /** Search variants: the cleaned query plus every synonym expansion of its tokens. */
    public Set<String> expandTerms(String query) {
        Set<String> variants = new LinkedHashSet<>();
        String cleaned = String.join(" ", tokens(query));
        if (!cleaned.isEmpty()) {
            variants.add(cleaned);
        }
        for (String token : tokens(query)) {
            variants.addAll(synonymService.expandTerm(token));
        }
        return variants;
    }
}
