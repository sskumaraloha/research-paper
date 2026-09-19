package com.mip.entry.service;

import com.mip.common.util.TextNormalizer;
import com.mip.dictionary.entity.FailureMode;
import com.mip.dictionary.service.FailureModeService;
import com.mip.importjob.service.NormalizationService;
import com.mip.machine.service.MachineResolution;
import com.mip.machine.service.MachineResolverService;
import com.mip.part.entity.SparePart;
import com.mip.part.repository.SparePartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls record fields out of a chat message: machine (n-gram resolution),
 * date (relative words or explicit dates), downtime (number + unit only, so a
 * machine code's digits are never mistaken for minutes), failure mode and parts.
 */
@Service
@RequiredArgsConstructor
public class EntryExtractionService {

    public record ExtractedFields(
            MachineResolution machine,
            LocalDate date,
            Integer downtimeMinutes,
            FailureMode failureMode,
            List<SparePart> parts
    ) {
    }

    private static final Pattern DURATION = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(hours|hour|hrs|hr|h|minutes|minute|mins|min|m)\\b");
    private static final Pattern EXPLICIT_DATE = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4})");

    private final MachineResolverService machineResolverService;
    private final FailureModeService failureModeService;
    private final SparePartRepository sparePartRepository;
    private final NormalizationService normalizationService;

    @Transactional(readOnly = true)
    public ExtractedFields extract(Long plantId, String message) {
        return new ExtractedFields(
                extractMachine(plantId, message).orElse(null),
                extractDate(message),
                extractDowntime(message),
                failureModeService.resolveFailureMode(message).orElse(null),
                extractParts(message));
    }

    private Optional<MachineResolution> extractMachine(Long plantId, String message) {
        List<String> tokens = List.of(TextNormalizer.normalize(message).split(" "));
        MachineResolution best = null;
        for (int size = Math.min(4, tokens.size()); size >= 1; size--) {
            for (int start = 0; start + size <= tokens.size(); start++) {
                String ngram = String.join(" ", tokens.subList(start, start + size));
                if (ngram.length() < 3) {
                    continue;
                }
                Optional<MachineResolution> resolution = machineResolverService.resolve(plantId, ngram);
                if (resolution.isPresent()
                        && (best == null || resolution.get().confidence() > best.confidence())) {
                    best = resolution.get();
                }
            }
            if (best != null && best.confidence() >= 0.9) {
                break;
            }
        }
        return best != null && best.confidence() >= 0.8 ? Optional.of(best) : Optional.empty();
    }

    LocalDate extractDate(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("day before yesterday")) {
            return LocalDate.now().minusDays(2);
        }
        if (lower.contains("yesterday")) {
            return LocalDate.now().minusDays(1);
        }
        if (lower.contains("today") || lower.contains("this morning") || lower.contains("just now")
                || lower.contains("tonight") || lower.contains("this evening")) {
            return LocalDate.now();
        }
        Matcher matcher = EXPLICIT_DATE.matcher(message);
        while (matcher.find()) {
            LocalDate parsed = normalizationService.parseDate(matcher.group(1));
            if (parsed != null && !parsed.isAfter(LocalDate.now())) {
                return parsed;
            }
        }
        return null;
    }

    Integer extractDowntime(String message) {
        Matcher matcher = DURATION.matcher(message.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
            String unit = matcher.group(2);
            int minutes = unit.startsWith("h") ? (int) Math.round(value * 60) : (int) Math.round(value);
            if (minutes > 0) {
                return minutes;
            }
        }
        return null;
    }

    private List<SparePart> extractParts(String message) {
        String normalized = TextNormalizer.normalize(message);
        List<SparePart> parts = new ArrayList<>();
        for (SparePart part : sparePartRepository.findAll()) {
            String name = TextNormalizer.normalize(part.getName());
            String number = TextNormalizer.normalize(part.getPartNumber());
            if ((!name.isEmpty() && normalized.contains(name))
                    || (!number.isEmpty() && normalized.contains(number))) {
                parts.add(part);
            }
        }
        return parts;
    }
}
