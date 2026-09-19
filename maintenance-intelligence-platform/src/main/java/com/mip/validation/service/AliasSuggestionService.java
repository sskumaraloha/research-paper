package com.mip.validation.service;

import com.mip.exception.InvalidStateTransitionException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.importjob.entity.StagedRow;
import com.mip.importjob.service.ConfidenceScoringService;
import com.mip.importjob.service.NormalizedRow;
import com.mip.machine.entity.AliasSource;
import com.mip.machine.entity.AliasSuggestion;
import com.mip.machine.entity.Machine;
import com.mip.machine.repository.AliasSuggestionRepository;
import com.mip.machine.repository.MachineRepository;
import com.mip.machine.service.MachineAliasService;
import com.mip.plant.service.PlantService;
import com.mip.security.MipUserDetails;
import com.mip.validation.dto.AliasMappingResponse;
import com.mip.validation.dto.AliasSuggestionResponse;
import com.mip.validation.dto.MapAliasRequest;
import com.mip.validation.entity.ValidationItem;
import com.mip.validation.repository.ValidationItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AliasSuggestionService {

    private final AliasSuggestionRepository suggestionRepository;
    private final ValidationItemRepository validationItemRepository;
    private final MachineRepository machineRepository;
    private final MachineAliasService machineAliasService;
    private final ConfidenceScoringService confidenceScoringService;
    private final PlantService plantService;

    @Transactional(readOnly = true)
    public List<AliasSuggestionResponse> listPending(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        return suggestionRepository.findByPlantIdAndStatusOrderByOccurrencesDesc(plantId,
                        AliasSuggestion.SuggestionStatus.PENDING).stream()
                .map(s -> new AliasSuggestionResponse(s.getId(), s.getRawText(), s.getOccurrences(),
                        s.getSuggestedMachine() == null ? null : s.getSuggestedMachine().getId(),
                        s.getSuggestedMachine() == null ? null : s.getSuggestedMachine().getName(),
                        s.getConfidence(), s.getStatus().name()))
                .toList();
    }

    /**
     * Maps an unresolved import name to a machine: stores the alias for future imports
     * and re-resolves every pending validation item that carried the unresolved text.
     */
    @Transactional
    public AliasMappingResponse mapAlias(Long suggestionId, MapAliasRequest request,
                                         MipUserDetails principal) {
        AliasSuggestion suggestion = suggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Alias suggestion", suggestionId));
        Long plantId = suggestion.getPlant().getId();
        plantService.requireAccessiblePlant(plantId, principal);
        if (suggestion.getStatus() != AliasSuggestion.SuggestionStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "This suggestion has already been " + suggestion.getStatus().name().toLowerCase());
        }
        Machine machine = machineRepository.findByIdAndPlantId(request.machineId(), plantId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine", request.machineId()));

        machineAliasService.addAliasIfAbsent(machine, suggestion.getRawText(), AliasSource.SUGGESTION);
        suggestion.setStatus(AliasSuggestion.SuggestionStatus.MAPPED);
        suggestion.setSuggestedMachine(machine);

        int revalidated = 0;
        for (ValidationItem item : validationItemRepository
                .findPendingWithUnresolvedMachine(plantId)) {
            StagedRow row = item.getStagedRow();
            // machineText is the raw file value; the suggestion stores the normalised form
            if (!com.mip.common.util.TextNormalizer.normalize(row.getMachineText())
                    .equals(suggestion.getRawText())) {
                continue;
            }
            row.setMachine(machine);
            row.setMachineConfidence(0.95);
            row.setResolutionMethod("ALIAS");
            row.setConfidence(confidenceScoringService.score(row.getMachineConfidence(),
                    row.getFailureModeConfidence(), asNormalizedRow(row)));
            revalidated++;
        }
        log.info("Alias suggestion {} mapped to machine {} ({} items re-resolved)",
                suggestionId, machine.getCode(), revalidated);
        return new AliasMappingResponse(suggestion.getId(), machine.getId(),
                suggestion.getRawText(), revalidated);
    }

    private NormalizedRow asNormalizedRow(StagedRow row) {
        return new NormalizedRow(row.getMachineText(), row.getParsedDate(), row.getDowntimeMinutes(),
                row.getDescription(), row.getActionTaken(), row.getTechnician(),
                row.getFailureModeText(), row.getPartsText(), List.of());
    }
}
