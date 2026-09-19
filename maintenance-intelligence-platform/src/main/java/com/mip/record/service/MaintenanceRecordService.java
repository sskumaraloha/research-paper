package com.mip.record.service;

import com.mip.common.dto.NamedRef;
import com.mip.common.dto.PageResponse;
import com.mip.audit.service.AuditService;
import com.mip.dictionary.entity.FailureMode;
import com.mip.dictionary.repository.FailureModeRepository;
import com.mip.dictionary.service.FailureModeService;
import com.mip.exception.InternalServerErrorException;
import com.mip.exception.InvalidRequestException;
import com.mip.exception.InvalidStateTransitionException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.machine.entity.Machine;
import com.mip.machine.repository.MachineRepository;
import com.mip.notification.service.NotificationService;
import com.mip.part.entity.SparePart;
import com.mip.part.service.SparePartService;
import com.mip.plant.repository.ProductionLineRepository;
import com.mip.plant.service.PlantService;
import com.mip.record.dto.CreateRecordRequest;
import com.mip.record.dto.RecordDetailResponse;
import com.mip.record.dto.RecordFilterOptionsResponse;
import com.mip.record.dto.RecordRowResponse;
import com.mip.record.dto.RejectRequest;
import com.mip.record.dto.SourceDocumentResponse;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordSource;
import com.mip.record.entity.RecordStatus;
import com.mip.record.mapper.RecordMapper;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.record.repository.SourceDocumentRepository;
import com.mip.security.MipUserDetails;
import com.mip.user.entity.User;
import com.mip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository recordRepository;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final MachineRepository machineRepository;
    private final ProductionLineRepository lineRepository;
    private final FailureModeRepository failureModeRepository;
    private final FailureModeService failureModeService;
    private final SparePartService sparePartService;
    private final PlantService plantService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final RecordMapper recordMapper;

    @Transactional(readOnly = true)
    public PageResponse<RecordRowResponse> listRecords(Long plantId, Long machineId, Long lineId,
                                                       Long failureModeId, LocalDate from, LocalDate to,
                                                       String text, int page, int size,
                                                       MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidRequestException("'from' date must not be after 'to' date");
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "recordDate", "id"));
        String textFilter = (text == null || text.isBlank()) ? null : text.trim();
        return PageResponse.of(recordRepository.search(plantId, RecordStatus.ACTIVE, machineId,
                lineId, failureModeId, from, to, textFilter, pageable), recordMapper::toRow);
    }

    @Transactional(readOnly = true)
    public RecordDetailResponse getRecord(Long recordId, MipUserDetails principal) {
        return recordMapper.toDetail(requireAccessibleRecord(recordId, principal));
    }

    /** CSV of the filtered records (same filters as the listing), capped at 10,000 rows. */
    @Transactional(readOnly = true)
    public String exportCsv(Long plantId, Long machineId, Long lineId, Long failureModeId,
                            LocalDate from, LocalDate to, String text, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        String textFilter = (text == null || text.isBlank()) ? null : text.trim();
        var records = recordRepository.search(plantId, RecordStatus.ACTIVE, machineId, lineId,
                failureModeId, from, to, textFilter,
                PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "recordDate", "id")));
        try (StringWriter out = new StringWriter();
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.builder()
                     .setHeader("id", "date", "machineCode", "machineName", "line", "failureMode",
                             "downtimeMinutes", "technician", "source", "confidence",
                             "description", "actionTaken", "spareParts")
                     .build())) {
            for (MaintenanceRecord record : records) {
                printer.printRecord(
                        record.getId(), record.getRecordDate(),
                        record.getMachine().getCode(), record.getMachine().getName(),
                        record.getMachine().getLine() == null ? ""
                                : record.getMachine().getLine().getName(),
                        record.getFailureMode() == null ? "" : record.getFailureMode().getName(),
                        record.getDowntimeMinutes(), nullToEmpty(record.getTechnician()),
                        record.getSource().name(),
                        record.getConfidence() == null ? "" : record.getConfidence(),
                        record.getDescription(), nullToEmpty(record.getActionTaken()),
                        record.getSpareParts().stream().map(SparePart::getName).sorted()
                                .collect(Collectors.joining("; ")));
            }
            printer.flush();
            return out.toString();
        } catch (IOException ex) {
            throw new InternalServerErrorException("CSV export failed: " + ex.getMessage());
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Transactional(readOnly = true)
    public PageResponse<RecordRowResponse> getMachineTimeline(Long machineId, int page, int size,
                                                              MipUserDetails principal) {
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("Machine", machineId));
        plantService.requireAccessiblePlant(machine.getPlant().getId(), principal);
        return PageResponse.of(recordRepository.findByMachineIdAndStatusOrderByRecordDateDescIdDesc(
                machineId, RecordStatus.ACTIVE, PageRequest.of(page, Math.min(size, 100))),
                recordMapper::toRow);
    }

    @Transactional
    public RecordDetailResponse createRecord(CreateRecordRequest request, MipUserDetails principal) {
        plantService.requireAccessiblePlant(request.plantId(), principal);
        Machine machine = machineRepository.findByIdAndPlantId(request.machineId(), request.plantId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine", request.machineId()));

        User creator = userService.getUser(principal.getId());
        MaintenanceRecord record = buildRecord(machine, request, RecordSource.MANUAL, creator, null);
        MaintenanceRecord saved = recordRepository.save(record);
        notificationService.onRecordCreated(saved);
        log.info("Manual record {} created on machine {} by user {}", saved.getId(),
                machine.getCode(), creator.getId());
        return recordMapper.toDetail(saved);
    }

    /** Record creation from a completed preventive-maintenance schedule. */
    @Transactional
    public MaintenanceRecord createPreventiveRecord(Machine machine, java.time.LocalDate performedOn,
                                                    int downtimeMinutes, String description,
                                                    String actionTaken, String technician, User creator) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setPlant(machine.getPlant());
        record.setMachine(machine);
        record.setRecordDate(performedOn);
        record.setDowntimeMinutes(downtimeMinutes);
        record.setDescription(description);
        record.setActionTaken(trimOrNull(actionTaken));
        record.setTechnician(trimOrNull(technician));
        record.setSource(RecordSource.PREVENTIVE);
        record.setCreatedBy(creator);
        MaintenanceRecord saved = recordRepository.save(record);
        log.info("Preventive record {} created on machine {}", saved.getId(), machine.getCode());
        return saved;
    }

    /** Record creation on behalf of the entry agent; the conversation already checked plant access. */
    @Transactional
    public MaintenanceRecord createRecordFromAgent(CreateRecordRequest request, User creator) {
        Machine machine = machineRepository.findByIdAndPlantId(request.machineId(), request.plantId())
                .orElseThrow(() -> new ResourceNotFoundException("Machine", request.machineId()));
        MaintenanceRecord saved = recordRepository.save(
                buildRecord(machine, request, RecordSource.ENTRY_AGENT, creator, null));
        notificationService.onRecordCreated(saved);
        log.info("Entry-agent record {} created on machine {} by user {}", saved.getId(),
                machine.getCode(), creator.getId());
        return saved;
    }

    /** Shared by manual entry and the entry agent; the import pipeline builds records itself. */
    @Transactional
    public MaintenanceRecord buildRecord(Machine machine, CreateRecordRequest request,
                                         RecordSource source, User creator, Double confidence) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setPlant(machine.getPlant());
        record.setMachine(machine);
        record.setRecordDate(request.recordDate());
        record.setDowntimeMinutes(request.downtimeMinutes());
        record.setDescription(request.description().trim());
        record.setActionTaken(trimOrNull(request.actionTaken()));
        record.setTechnician(trimOrNull(request.technician()));
        record.setSource(source);
        record.setCreatedBy(creator);
        record.setConfidence(confidence);
        record.setFailureMode(resolveFailureMode(request));
        if (request.partNames() != null) {
            for (String partName : request.partNames()) {
                record.getSpareParts().add(sparePartService.resolveOrCreatePart(partName));
            }
        }
        return record;
    }

    @Transactional
    public RecordDetailResponse rejectRecord(Long recordId, RejectRequest request,
                                             MipUserDetails principal) {
        MaintenanceRecord record = requireAccessibleRecord(recordId, principal);
        if (record.getStatus() == RecordStatus.REJECTED) {
            throw new InvalidStateTransitionException("Record is already rejected");
        }
        record.setStatus(RecordStatus.REJECTED);
        record.setRejectedReason(request.reason().trim());
        auditService.log(principal, "RECORD_REJECTED", "RECORD", record.getId(),
                record.getPlant().getId(), request.reason());
        log.info("Record {} rejected by user {}: {}", recordId, principal.getId(), request.reason());
        return recordMapper.toDetail(record);
    }

    @Transactional(readOnly = true)
    public RecordFilterOptionsResponse listFilterOptions(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        List<NamedRef> machines = machineRepository.findByPlantIdAndActiveTrue(plantId).stream()
                .map(m -> new NamedRef(m.getId(), m.getName()))
                .sorted(Comparator.comparing(NamedRef::name))
                .toList();
        List<NamedRef> lines = lineRepository.findByPlantIdOrderByCodeAsc(plantId).stream()
                .map(l -> new NamedRef(l.getId(), l.getName()))
                .toList();
        List<NamedRef> failureModes = listDistinctFailureModesInUse(plantId);
        return new RecordFilterOptionsResponse(machines, lines, failureModes);
    }

    @Transactional(readOnly = true)
    public List<NamedRef> listDistinctFailureModesInUse(Long plantId) {
        return recordRepository.findDistinctFailureModesInUse(plantId).stream()
                .map(fm -> new NamedRef(fm.getId(), fm.getName()))
                .sorted(Comparator.comparing(NamedRef::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SourceDocumentResponse> listSourceDocuments(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        return sourceDocumentRepository.findByPlantIdOrderByCreatedAtDesc(plantId).stream()
                .map(doc -> new SourceDocumentResponse(doc.getId(), doc.getFilename(),
                        doc.getContentType(), doc.getSizeBytes(),
                        doc.getUploadedBy().getFullName(), doc.getCreatedAt()))
                .toList();
    }

    private MaintenanceRecord requireAccessibleRecord(Long recordId, MipUserDetails principal) {
        MaintenanceRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record", recordId));
        plantService.requireAccessiblePlant(record.getPlant().getId(), principal);
        return record;
    }

    private FailureMode resolveFailureMode(CreateRecordRequest request) {
        if (request.failureModeId() != null) {
            return failureModeRepository.findById(request.failureModeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Failure mode",
                            request.failureModeId()));
        }
        String text = request.failureModeText() == null || request.failureModeText().isBlank()
                ? request.description()
                : request.failureModeText();
        return failureModeService.resolveFailureMode(text).orElse(null);
    }

    private String trimOrNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
