package com.mip.machine.service;

import com.mip.common.util.TextNormalizer;
import com.mip.dictionary.entity.SynonymDomain;
import com.mip.dictionary.service.SynonymService;
import com.mip.machine.entity.Machine;
import com.mip.machine.entity.MachineAlias;
import com.mip.machine.repository.MachineAliasRepository;
import com.mip.machine.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Resolves the free-text machine references found in import files and chat messages
 * to actual machines. Deterministic tiers: exact code, exact name, stored alias,
 * synonym-corrected retry, then fuzzy similarity as the last resort.
 */
@Service
@RequiredArgsConstructor
public class MachineResolverService {

    /** Fuzzy matches below this similarity are not proposed at all. */
    private static final double FUZZY_FLOOR = 0.72;

    private final MachineRepository machineRepository;
    private final MachineAliasRepository aliasRepository;
    private final SynonymService synonymService;

    @Transactional(readOnly = true)
    public Optional<MachineResolution> resolve(Long plantId, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        String needle = TextNormalizer.normalize(rawText);
        if (needle.isEmpty()) {
            return Optional.empty();
        }

        List<Machine> machines = machineRepository.findByPlantIdAndActiveTrue(plantId);

        Optional<MachineResolution> direct = matchDirect(machines, plantId, needle, false);
        if (direct.isPresent()) {
            return direct;
        }

        String canonical = synonymService.canonicalise(needle, SynonymDomain.MACHINE);
        if (!canonical.equals(needle)) {
            Optional<MachineResolution> viaSynonym = matchDirect(machines, plantId,
                    TextNormalizer.normalize(canonical), true);
            if (viaSynonym.isPresent()) {
                return viaSynonym;
            }
        }

        return fuzzyMatch(machines, plantId, needle, FUZZY_FLOOR);
    }

    /**
     * Best candidate even below the resolution threshold, for alias suggestions.
     * Still requires minimal similarity so pure noise yields no suggestion.
     */
    @Transactional(readOnly = true)
    public Optional<MachineResolution> suggest(Long plantId, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Optional.empty();
        }
        String needle = TextNormalizer.normalize(rawText);
        if (needle.isEmpty()) {
            return Optional.empty();
        }
        return fuzzyMatch(machineRepository.findByPlantIdAndActiveTrue(plantId), plantId, needle, 0.4);
    }

    private Optional<MachineResolution> matchDirect(List<Machine> machines, Long plantId,
                                                    String needle, boolean viaSynonym) {
        for (Machine machine : machines) {
            if (TextNormalizer.normalize(machine.getCode()).equals(needle)) {
                return Optional.of(new MachineResolution(machine, viaSynonym ? 0.92 : 1.0,
                        viaSynonym ? MachineResolution.ResolutionMethod.SYNONYM
                                : MachineResolution.ResolutionMethod.EXACT_CODE));
            }
        }
        for (Machine machine : machines) {
            if (TextNormalizer.normalize(machine.getName()).equals(needle)) {
                return Optional.of(new MachineResolution(machine, viaSynonym ? 0.9 : 0.97,
                        viaSynonym ? MachineResolution.ResolutionMethod.SYNONYM
                                : MachineResolution.ResolutionMethod.EXACT_NAME));
            }
        }
        return aliasRepository.findByPlantIdAndAlias(plantId, needle)
                .map(MachineAlias::getMachine)
                .filter(Machine::isActive)
                .map(machine -> new MachineResolution(machine, 0.95,
                        MachineResolution.ResolutionMethod.ALIAS));
    }

    private Optional<MachineResolution> fuzzyMatch(List<Machine> machines, Long plantId, String needle,
                                                   double floor) {
        Machine best = null;
        double bestSimilarity = 0;
        for (Machine machine : machines) {
            double similarity = Math.max(
                    TextNormalizer.similarity(needle, TextNormalizer.normalize(machine.getCode())),
                    TextNormalizer.similarity(needle, TextNormalizer.normalize(machine.getName())));
            if (similarity > bestSimilarity) {
                best = machine;
                bestSimilarity = similarity;
            }
        }
        for (MachineAlias alias : aliasRepository.findByPlantId(plantId)) {
            double similarity = TextNormalizer.similarity(needle, alias.getAlias());
            if (similarity > bestSimilarity && alias.getMachine().isActive()) {
                best = alias.getMachine();
                bestSimilarity = similarity;
            }
        }
        if (best == null || bestSimilarity < floor) {
            return Optional.empty();
        }
        // map similarity [floor,1] onto confidence [0.5,0.9]
        double confidence = 0.5 + 0.4 * (bestSimilarity - floor) / (1 - floor);
        return Optional.of(new MachineResolution(best, confidence,
                MachineResolution.ResolutionMethod.FUZZY));
    }
}
