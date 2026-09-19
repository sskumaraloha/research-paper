package com.mip.machine.service;

import com.mip.common.util.TextNormalizer;
import com.mip.exception.DuplicateResourceException;
import com.mip.exception.InvalidRequestException;
import com.mip.machine.dto.AliasResponse;
import com.mip.machine.entity.AliasSource;
import com.mip.machine.entity.Machine;
import com.mip.machine.entity.MachineAlias;
import com.mip.machine.repository.MachineAliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MachineAliasService {

    private final MachineAliasRepository aliasRepository;
    private final MachineResolverService machineResolverService;

    @Transactional(readOnly = true)
    public List<AliasResponse> listAliases(Long machineId) {
        return aliasRepository.findByMachineIdOrderByAliasAsc(machineId).stream()
                .map(a -> new AliasResponse(a.getId(), a.getAlias(), a.getSource().name()))
                .toList();
    }

    /** Resolves free text to a machine within a plant; empty when nothing matches confidently. */
    @Transactional(readOnly = true)
    public Optional<MachineResolution> resolveMachine(Long plantId, String text) {
        return machineResolverService.resolve(plantId, text);
    }

    @Transactional
    public AliasResponse addAlias(Machine machine, String rawAlias, AliasSource source) {
        String alias = TextNormalizer.normalize(rawAlias);
        if (alias.isEmpty()) {
            throw new InvalidRequestException("Alias must contain letters or digits");
        }
        Long plantId = machine.getPlant().getId();
        if (aliasRepository.existsByPlantIdAndAlias(plantId, alias)) {
            throw new DuplicateResourceException("Alias '" + alias + "' already exists in this plant");
        }
        MachineAlias saved = aliasRepository.save(new MachineAlias(machine, alias, source));
        return new AliasResponse(saved.getId(), saved.getAlias(), saved.getSource().name());
    }

    /** Idempotent variant used by the import pipeline and alias mapping. */
    @Transactional
    public void addAliasIfAbsent(Machine machine, String rawAlias, AliasSource source) {
        String alias = TextNormalizer.normalize(rawAlias);
        if (alias.isEmpty() || aliasRepository.existsByPlantIdAndAlias(machine.getPlant().getId(), alias)) {
            return;
        }
        aliasRepository.save(new MachineAlias(machine, alias, source));
    }
}
