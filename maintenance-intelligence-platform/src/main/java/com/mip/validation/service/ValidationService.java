package com.mip.validation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mip.audit.service.AuditService;
import com.mip.common.dto.PageResponse;
import com.mip.dictionary.repository.FailureModeRepository;
import com.mip.exception.BusinessRuleViolationException;
import com.mip.exception.InvalidStateTransitionException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.importjob.entity.StagedRow;
import com.mip.importjob.entity.StagedRowStatus;
import com.mip.importjob.service.ImportPipelineService;
import com.mip.machine.entity.AliasSource;
import com.mip.machine.entity.AliasSuggestion;
import com.mip.machine.entity.Machine;
import com.mip.machine.repository.AliasSuggestionRepository;
import com.mip.machine.repository.MachineRepository;
import com.mip.machine.service.MachineAliasService;
import com.mip.common.util.TextNormalizer;
import com.mip.plant.service.PlantService;
import com.mip.record.dto.RejectRequest;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.security.MipUserDetails;
import com.mip.user.service.UserService;
import com.mip.validation.dto.EditValidationItemRequest;
import com.mip.validation.dto.ValidationDecisionResponse;
import com.mip.validation.dto.ValidationItemResponse;
import com.mip.validation.dto.ValidationQueueResponse;
import com.mip.validation.entity.ValidationItem;
import com.mip.validation.repository.ValidationItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    private final ValidationItemRepository validationItemRepository;
    private final MachineRepository machineRepository;
    private final FailureModeRepository failureModeRepository;
    private final AliasSuggestionRepository aliasSuggestionRepository;
    private final MachineAliasService machineAliasService;
    private final ImportPipelineService pipelineService;
    private final PlantService plantService;
    private final UserService userService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ValidationQueueResponse getQueue(Long plantId, int page, int size, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        long pending = validationItemRepository.countByPlantIdAndStatus(plantId,
                ValidationItem.ValidationStatus.PENDING);
        var items = validationItemRepository.findByPlantIdAndStatusOrderByCreatedAtAsc(plantId,
                        ValidationItem.ValidationStatus.PENDING,
                        PageRequest.of(page, Math.min(size, 100)))
                .map(this::toResponse);
        return new ValidationQueueResponse(pending, PageResponse.of(items));
    }

    @Transactional
    public ValidationDecisionResponse approve(Long itemId, MipUserDetails principal) {
        ValidationItem item = requirePendingItem(itemId, principal);
        MaintenanceRecord record = importRow(item, principal);
        return new ValidationDecisionResponse(item.getId(), item.getStatus().name(), record.getId());
    }

    @Transactional
    public ValidationDecisionResponse editAndApprove(Long itemId, EditValidationItemRequest request,
                                                     MipUserDetails principal) {
        ValidationItem item = requirePendingItem(itemId, principal);
        applyEdits(item, request);
        MaintenanceRecord record = importRow(item, principal);
        return new ValidationDecisionResponse(item.getId(), item.getStatus().name(), record.getId());
    }

    @Transactional
    public ValidationDecisionResponse reject(Long itemId, RejectRequest request,
                                             MipUserDetails principal) {
        ValidationItem item = requirePendingItem(itemId, principal);
        item.setStatus(ValidationItem.ValidationStatus.REJECTED);
        item.setDecidedBy(userService.getUser(principal.getId()));
        item.setDecidedAt(Instant.now());
        item.setDecisionNote(request.reason().trim());
        item.getStagedRow().setStatus(StagedRowStatus.REJECTED_BY_VALIDATOR);
        auditService.log(principal, "VALIDATION_REJECTED", "VALIDATION_ITEM", item.getId(),
                item.getPlant().getId(), request.reason());
        log.info("Validation item {} rejected by user {}", itemId, principal.getId());
        return new ValidationDecisionResponse(item.getId(), item.getStatus().name(), null);
    }

    @Transactional(readOnly = true)
    public long pendingCount(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        return validationItemRepository.countByPlantIdAndStatus(plantId,
                ValidationItem.ValidationStatus.PENDING);
    }

    private MaintenanceRecord importRow(ValidationItem item, MipUserDetails principal) {
        StagedRow row = item.getStagedRow();
        requireComplete(row);
        MaintenanceRecord record = pipelineService.createRecordFromRow(row.getJob(), row);
        row.setResultingRecord(record);
        row.setStatus(StagedRowStatus.IMPORTED_AFTER_VALIDATION);
        item.setStatus(ValidationItem.ValidationStatus.APPROVED);
        item.setDecidedBy(userService.getUser(principal.getId()));
        item.setDecidedAt(Instant.now());
        item.setResultingRecord(record);
        auditService.log(principal, "VALIDATION_APPROVED", "VALIDATION_ITEM", item.getId(),
                item.getPlant().getId(), "record " + record.getId());
        log.info("Validation item {} approved by user {}: record {}", item.getId(),
                principal.getId(), record.getId());
        return record;
    }

    private void applyEdits(ValidationItem item, EditValidationItemRequest request) {
        StagedRow row = item.getStagedRow();
        Long plantId = item.getPlant().getId();

        if (request.machineId() != null) {
            Machine machine = machineRepository.findByIdAndPlantId(request.machineId(), plantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Machine", request.machineId()));
            boolean wasUnresolved = row.getMachine() == null;
            row.setMachine(machine);
            row.setMachineConfidence(1.0);
            row.setResolutionMethod("HUMAN");
            if (wasUnresolved && row.getMachineText() != null) {
                learnAlias(machine, row.getMachineText());
            }
        }
        if (request.failureModeId() != null) {
            row.setFailureMode(failureModeRepository.findById(request.failureModeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Failure mode",
                            request.failureModeId())));
            row.setFailureModeConfidence(1.0);
        }
        if (request.recordDate() != null) {
            row.setParsedDate(request.recordDate());
        }
        if (request.downtimeMinutes() != null) {
            row.setDowntimeMinutes(request.downtimeMinutes());
        }
        if (request.description() != null && !request.description().isBlank()) {
            row.setDescription(request.description().trim());
        }
        if (request.actionTaken() != null && !request.actionTaken().isBlank()) {
            row.setActionTaken(request.actionTaken().trim());
        }
        if (request.technician() != null && !request.technician().isBlank()) {
            row.setTechnician(request.technician().trim());
        }
        if (request.partNames() != null && !request.partNames().isEmpty()) {
            row.setPartsText(String.join(",", request.partNames()));
        }
        // human review supersedes the pipeline's uncertainty
        row.setConfidence(1.0);
    }

    /** Learns the unresolved text as an alias and closes the matching suggestion. */
    private void learnAlias(Machine machine, String machineText) {
        machineAliasService.addAliasIfAbsent(machine, machineText, AliasSource.IMPORT);
        aliasSuggestionRepository.findByPlantIdAndRawText(machine.getPlant().getId(),
                        TextNormalizer.normalize(machineText))
                .filter(s -> s.getStatus() == AliasSuggestion.SuggestionStatus.PENDING)
                .ifPresent(suggestion -> {
                    suggestion.setStatus(AliasSuggestion.SuggestionStatus.MAPPED);
                    suggestion.setSuggestedMachine(machine);
                });
    }

    private void requireComplete(StagedRow row) {
        if (row.getMachine() == null) {
            throw new BusinessRuleViolationException(
                    "The machine is unresolved; supply machineId via edit-approve");
        }
        if (row.getParsedDate() == null || row.getDescription() == null) {
            throw new BusinessRuleViolationException(
                    "Record date and description are required; supply them via edit-approve");
        }
        if (row.getDowntimeMinutes() == null) {
            throw new BusinessRuleViolationException(
                    "Downtime is missing; supply downtimeMinutes via edit-approve");
        }
    }

    private ValidationItem requirePendingItem(Long itemId, MipUserDetails principal) {
        ValidationItem item = validationItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Validation item", itemId));
        plantService.requireAccessiblePlant(item.getPlant().getId(), principal);
        if (item.getStatus() != ValidationItem.ValidationStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "This item has already been " + item.getStatus().name().toLowerCase());
        }
        return item;
    }

    private ValidationItemResponse toResponse(ValidationItem item) {
        StagedRow row = item.getStagedRow();
        return new ValidationItemResponse(
                item.getId(),
                row.getJob().getId(),
                row.getJob().getSourceDocument().getFilename(),
                row.getRowNumber(),
                Arrays.asList(item.getReasons().split(",")),
                row.getConfidence(),
                row.getMachineText(),
                row.getMachine() == null ? null : row.getMachine().getId(),
                row.getMachine() == null ? null : row.getMachine().getName(),
                row.getFailureMode() == null ? null : row.getFailureMode().getId(),
                row.getFailureMode() == null ? null : row.getFailureMode().getName(),
                row.getParsedDate(), row.getDowntimeMinutes(), row.getDescription(),
                row.getActionTaken(), row.getTechnician(), row.getPartsText(),
                parseRawData(row.getRawData()));
    }

    private Map<String, String> parseRawData(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
