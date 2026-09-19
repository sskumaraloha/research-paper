package com.mip.importjob.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mip.exception.FileProcessingException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.importjob.entity.ImportJob;
import com.mip.importjob.entity.ImportJobStatus;
import com.mip.importjob.entity.ImportJobStep;
import com.mip.importjob.entity.ImportStepName;
import com.mip.importjob.entity.StagedRow;
import com.mip.importjob.entity.StagedRowStatus;
import com.mip.importjob.repository.ImportJobRepository;
import com.mip.importjob.repository.ImportJobStepRepository;
import com.mip.importjob.repository.StagedRowRepository;
import com.mip.machine.entity.AliasSuggestion;
import com.mip.machine.repository.AliasSuggestionRepository;
import com.mip.machine.service.MachineResolution;
import com.mip.machine.service.MachineResolverService;
import com.mip.common.util.TextNormalizer;
import com.mip.part.service.SparePartService;
import com.mip.plant.entity.Plant;
import com.mip.record.entity.MaintenanceRecord;
import com.mip.record.entity.RecordSource;
import com.mip.record.repository.MaintenanceRecordRepository;
import com.mip.validation.entity.ValidationItem;
import com.mip.validation.repository.ValidationItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The staged import pipeline: PARSE → NORMALIZE → RESOLVE → EXTRACT → SCORE → FINALIZE.
 * Runs atomically: a crash anywhere rolls the whole run back, and the caller records
 * the failure via {@link #markFailed}. Finalising routes each row by the plant's
 * thresholds: auto-import, human validation queue, or outright rejection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportPipelineService {

    // reason codes surfaced to the validation queue
    public static final String REASON_UNRESOLVED_MACHINE = "UNRESOLVED_MACHINE";
    public static final String REASON_LOW_CONFIDENCE = "LOW_CONFIDENCE";
    public static final String REASON_NO_FAILURE_MODE = "NO_FAILURE_MODE";

    private final ImportJobRepository jobRepository;
    private final ImportJobStepRepository stepRepository;
    private final StagedRowRepository stagedRowRepository;
    private final ValidationItemRepository validationItemRepository;
    private final AliasSuggestionRepository aliasSuggestionRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final FileParserService fileParserService;
    private final NormalizationService normalizationService;
    private final MachineResolverService machineResolverService;
    private final FailureModeExtractorService failureModeExtractorService;
    private final ConfidenceScoringService confidenceScoringService;
    private final SparePartService sparePartService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void execute(Long jobId) {
        ImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job", jobId));
        job.setStatus(ImportJobStatus.RUNNING);
        job.setStartedAt(Instant.now());

        int order = 0;

        // PARSE
        ImportJobStep parseStep = startStep(job, ImportStepName.PARSE, order++);
        ParsedFile parsed = parseDocument(job);
        List<StagedRow> rows = new ArrayList<>();
        for (int i = 0; i < parsed.rows().size(); i++) {
            rows.add(stagedRowRepository.save(new StagedRow(job, i + 2 /* 1-based, after header */,
                    toJson(parsed.rows().get(i)))));
        }
        job.setTotalRows(rows.size());
        finishStep(parseStep, rows.size(), "Parsed " + rows.size() + " data rows");

        // NORMALIZE
        ImportJobStep normalizeStep = startStep(job, ImportStepName.NORMALIZE, order++);
        List<NormalizedRow> normalized = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            NormalizedRow n = normalizationService.normalize(parsed.rows().get(i));
            normalized.add(n);
            StagedRow row = rows.get(i);
            row.setMachineText(n.machineText());
            row.setParsedDate(n.date());
            row.setDowntimeMinutes(n.downtimeMinutes());
            row.setDescription(n.description());
            row.setActionTaken(n.actionTaken());
            row.setTechnician(n.technician());
            row.setFailureModeText(n.failureModeText());
            row.setPartsText(n.partsText());
        }
        finishStep(normalizeStep, rows.size(), null);

        // RESOLVE machines
        ImportJobStep resolveStep = startStep(job, ImportStepName.RESOLVE, order++);
        Long plantId = job.getPlant().getId();
        int resolved = 0;
        for (StagedRow row : rows) {
            Optional<MachineResolution> resolution =
                    machineResolverService.resolve(plantId, row.getMachineText());
            if (resolution.isPresent()) {
                row.setMachine(resolution.get().machine());
                row.setMachineConfidence(resolution.get().confidence());
                row.setResolutionMethod(resolution.get().method().name());
                resolved++;
            }
        }
        finishStep(resolveStep, rows.size(), resolved + " of " + rows.size() + " machines resolved");

        // EXTRACT failure modes
        ImportJobStep extractStep = startStep(job, ImportStepName.EXTRACT, order++);
        int extracted = 0;
        for (StagedRow row : rows) {
            Optional<FailureModeExtractorService.ExtractionResult> extraction =
                    failureModeExtractorService.extract(row.getFailureModeText(), row.getDescription());
            if (extraction.isPresent()) {
                row.setFailureMode(extraction.get().failureMode());
                row.setFailureModeConfidence(extraction.get().confidence());
                extracted++;
            }
        }
        finishStep(extractStep, rows.size(), extracted + " failure modes extracted");

        // SCORE
        ImportJobStep scoreStep = startStep(job, ImportStepName.SCORE, order++);
        for (int i = 0; i < rows.size(); i++) {
            StagedRow row = rows.get(i);
            row.setConfidence(confidenceScoringService.score(row.getMachineConfidence(),
                    row.getFailureModeConfidence(), normalized.get(i)));
        }
        finishStep(scoreStep, rows.size(), null);

        // FINALIZE: route every row
        ImportJobStep finalizeStep = startStep(job, ImportStepName.FINALIZE, order);
        for (StagedRow row : rows) {
            routeRow(job, row);
        }
        finishStep(finalizeStep, rows.size(), summary(job));

        job.setStatus(job.getInvalidCount() + job.getRejectedCount() > 0
                ? ImportJobStatus.COMPLETED_WITH_ERRORS
                : ImportJobStatus.COMPLETED);
        job.setFinishedAt(Instant.now());
        log.info("Import job {} finished: {}", job.getId(), summary(job));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long jobId, String message) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(ImportJobStatus.FAILED);
            job.setErrorMessage(message == null ? "Import failed" : message);
            job.setFinishedAt(Instant.now());
        });
    }

    private void routeRow(ImportJob job, StagedRow row) {
        Plant plant = job.getPlant();
        List<String> reasons = new ArrayList<>();

        boolean unusable = row.getMachineText() == null && row.getDescription() == null;
        if (unusable) {
            row.setStatus(StagedRowStatus.INVALID);
            row.setReasons("EMPTY_ROW");
            job.setInvalidCount(job.getInvalidCount() + 1);
            return;
        }

        if (row.getMachine() == null) {
            reasons.add(REASON_UNRESOLVED_MACHINE);
            if (row.getMachineText() != null) {
                upsertAliasSuggestion(plant, row.getMachineText());
            }
        }
        if (row.getFailureMode() == null) {
            reasons.add(REASON_NO_FAILURE_MODE);
        }
        if (row.getParsedDate() == null) {
            reasons.add("MISSING_DATE");
        }
        if (row.getDowntimeMinutes() == null) {
            reasons.add("MISSING_DOWNTIME");
        }
        if (row.getDescription() == null) {
            reasons.add("MISSING_DESCRIPTION");
        }

        double confidence = row.getConfidence() == null ? 0 : row.getConfidence();
        boolean complete = row.getMachine() != null && row.getParsedDate() != null
                && row.getDowntimeMinutes() != null && row.getDescription() != null;

        if (complete && confidence >= plant.getAutoApproveThreshold()) {
            MaintenanceRecord record = createRecordFromRow(job, row);
            row.setResultingRecord(record);
            row.setStatus(StagedRowStatus.AUTO_IMPORTED);
            job.setAutoImportedCount(job.getAutoImportedCount() + 1);
            return;
        }

        if (confidence < plant.getLowConfidenceThreshold() && row.getMachine() == null) {
            row.setStatus(StagedRowStatus.REJECTED_LOW_CONFIDENCE);
            row.setReasons(String.join(",", reasons));
            job.setRejectedCount(job.getRejectedCount() + 1);
            return;
        }

        if (reasons.isEmpty()) {
            reasons.add(REASON_LOW_CONFIDENCE);
        }
        row.setStatus(StagedRowStatus.NEEDS_VALIDATION);
        row.setReasons(String.join(",", reasons));
        validationItemRepository.save(new ValidationItem(row, plant, String.join(",", reasons)));
        job.setNeedsValidationCount(job.getNeedsValidationCount() + 1);
    }

    /** Builds the actual maintenance record for a fully-resolved row (auto-import and validation approve). */
    @Transactional
    public MaintenanceRecord createRecordFromRow(ImportJob job, StagedRow row) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setPlant(job.getPlant());
        record.setMachine(row.getMachine());
        record.setFailureMode(row.getFailureMode());
        record.setRecordDate(row.getParsedDate());
        record.setDowntimeMinutes(row.getDowntimeMinutes() == null ? 0 : row.getDowntimeMinutes());
        record.setDescription(row.getDescription());
        record.setActionTaken(row.getActionTaken());
        record.setTechnician(row.getTechnician());
        record.setSource(RecordSource.IMPORT);
        record.setConfidence(row.getConfidence());
        record.setSourceDocument(job.getSourceDocument());
        record.setCreatedBy(job.getTriggeredBy());
        if (row.getPartsText() != null && !row.getPartsText().isBlank()) {
            for (String part : row.getPartsText().split("[,;+/]")) {
                if (!part.isBlank()) {
                    record.getSpareParts().add(sparePartService.resolveOrCreatePart(part.trim()));
                }
            }
        }
        return recordRepository.save(record);
    }

    private void upsertAliasSuggestion(Plant plant, String machineText) {
        String normalized = TextNormalizer.normalize(machineText);
        if (normalized.isEmpty()) {
            return;
        }
        aliasSuggestionRepository.findByPlantIdAndRawText(plant.getId(), normalized)
                .ifPresentOrElse(existing -> {
                    if (existing.getStatus() == AliasSuggestion.SuggestionStatus.PENDING) {
                        existing.setOccurrences(existing.getOccurrences() + 1);
                    }
                }, () -> {
                    Optional<MachineResolution> guess =
                            machineResolverService.suggest(plant.getId(), normalized);
                    aliasSuggestionRepository.save(new AliasSuggestion(plant, normalized,
                            guess.map(MachineResolution::machine).orElse(null),
                            guess.map(MachineResolution::confidence).orElse(null)));
                });
    }

    private ParsedFile parseDocument(ImportJob job) {
        Path path = Path.of(job.getSourceDocument().getStoragePath());
        try (InputStream input = Files.newInputStream(path)) {
            return fileParserService.parse(input, job.getSourceDocument().getFilename());
        } catch (IOException ex) {
            throw new FileProcessingException("Stored file could not be read: " + ex.getMessage());
        }
    }

    private ImportJobStep startStep(ImportJob job, ImportStepName name, int orderIndex) {
        // honours the time limiter: an interrupted run aborts at the next step boundary,
        // rolling the whole pipeline back so markFailed records a clean FAILED state
        if (Thread.currentThread().isInterrupted()) {
            throw new FileProcessingException("Import aborted: time budget exceeded");
        }
        ImportJobStep step = new ImportJobStep(job, name, orderIndex);
        step.setStatus(ImportJobStep.StepStatus.RUNNING);
        step.setStartedAt(Instant.now());
        return stepRepository.save(step);
    }

    private void finishStep(ImportJobStep step, int processedCount, String message) {
        step.setStatus(ImportJobStep.StepStatus.COMPLETED);
        step.setProcessedCount(processedCount);
        step.setMessage(message);
        step.setFinishedAt(Instant.now());
    }

    private String summary(ImportJob job) {
        return job.getAutoImportedCount() + " auto-imported, "
                + job.getNeedsValidationCount() + " need validation, "
                + job.getRejectedCount() + " rejected, "
                + job.getInvalidCount() + " invalid";
    }

    private String toJson(Map<String, String> row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException ex) {
            throw new FileProcessingException("Row could not be serialised: " + ex.getMessage());
        }
    }
}
