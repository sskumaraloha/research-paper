package com.mip.assistant.service;

import com.mip.common.util.TextNormalizer;
import com.mip.dictionary.entity.FailureMode;
import com.mip.dictionary.service.FailureModeService;
import com.mip.machine.entity.Machine;
import com.mip.machine.service.MachineResolution;
import com.mip.machine.service.MachineResolverService;
import com.mip.part.entity.SparePart;
import com.mip.part.repository.SparePartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Deterministic intent classification: extract entities (machine via n-gram resolution,
 * failure mode via the dictionary, part by name containment), then pick the intent from
 * keyword patterns, most specific first.
 */
@Service
@RequiredArgsConstructor
public class IntentRouterService {

    private final MachineResolverService machineResolverService;
    private final FailureModeService failureModeService;
    private final SparePartRepository sparePartRepository;

    @Transactional(readOnly = true)
    public RoutedIntent route(Long plantId, String question) {
        String lower = question.toLowerCase(Locale.ROOT);
        Machine machine = extractMachine(plantId, question).orElse(null);
        FailureMode failureMode = failureModeService.resolveFailureMode(question).orElse(null);
        SparePart part = extractPart(question).orElse(null);

        if (containsAny(lower, "kpi", "kpis", "overview", "summary", "how are we doing",
                "plant performance", "this month")) {
            return new RoutedIntent(Intent.PLANT_KPIS, machine, failureMode, part);
        }
        if (containsAny(lower, "similar", "like this", "seen before", "happened before")) {
            return new RoutedIntent(Intent.SIMILAR_FAILURES, machine, failureMode, part);
        }
        if (containsAny(lower, "repeated", "recurring", "keeps failing", "keeps breaking",
                "again and again", "frequently fail")) {
            return new RoutedIntent(Intent.REPEATED_FAILURES, machine, failureMode, part);
        }
        if (containsAny(lower, "worst", "highest downtime", "most downtime", "top machine",
                "biggest problem", "most breakdowns")) {
            return new RoutedIntent(Intent.HIGHEST_DOWNTIME, machine, failureMode, part);
        }
        if (part != null || containsAny(lower, "spare part", "part usage", "parts used",
                "replaced part", "consumption")) {
            return new RoutedIntent(part != null ? Intent.PART_USAGE : Intent.RECORD_SEARCH,
                    machine, failureMode, part);
        }
        if (failureMode != null && containsAny(lower, "downtime", "impact", "cost", "how much",
                "total", "stats", "statistics")) {
            return new RoutedIntent(Intent.FAILURE_MODE_DOWNTIME, machine, failureMode, part);
        }
        if (machine != null) {
            return new RoutedIntent(Intent.MACHINE_HISTORY, machine, failureMode, part);
        }
        if (failureMode != null) {
            return new RoutedIntent(Intent.FAILURE_MODE_DOWNTIME, machine, failureMode, part);
        }
        if (TextNormalizer.normalize(question).split(" ").length >= 2) {
            return new RoutedIntent(Intent.RECORD_SEARCH, null, null, null);
        }
        return RoutedIntent.of(Intent.FALLBACK);
    }

    /** Slides 1–4 token n-grams over the question, longest first, and resolves each. */
    private Optional<Machine> extractMachine(Long plantId, String question) {
        List<String> tokens = List.of(TextNormalizer.normalize(question).split(" "));
        for (int size = Math.min(4, tokens.size()); size >= 1; size--) {
            for (int start = 0; start + size <= tokens.size(); start++) {
                String ngram = String.join(" ", tokens.subList(start, start + size));
                if (ngram.length() < 3) {
                    continue;
                }
                Optional<MachineResolution> resolution = machineResolverService.resolve(plantId, ngram);
                if (resolution.isPresent() && resolution.get().confidence() >= 0.9) {
                    return resolution.map(MachineResolution::machine);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<SparePart> extractPart(String question) {
        String normalizedQuestion = TextNormalizer.normalize(question);
        return sparePartRepository.findAll().stream()
                .filter(part -> {
                    String name = TextNormalizer.normalize(part.getName());
                    String number = TextNormalizer.normalize(part.getPartNumber());
                    return (!name.isEmpty() && normalizedQuestion.contains(name))
                            || (!number.isEmpty() && normalizedQuestion.contains(number));
                })
                .findFirst();
    }

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
