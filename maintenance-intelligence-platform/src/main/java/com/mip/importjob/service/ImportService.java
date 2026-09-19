package com.mip.importjob.service;

import com.mip.exception.BusinessRuleViolationException;
import com.mip.exception.DuplicateResourceException;
import com.mip.exception.FileProcessingException;
import com.mip.exception.InvalidRequestException;
import com.mip.exception.PayloadTooLargeException;
import com.mip.exception.ResourceNotFoundException;
import com.mip.importjob.config.ImportProperties;
import com.mip.importjob.dto.ImportJobResponse;
import com.mip.importjob.dto.ImportStepResponse;
import com.mip.importjob.dto.ImportSummaryResponse;
import com.mip.importjob.entity.ImportJob;
import com.mip.importjob.entity.ImportJobStatus;
import com.mip.importjob.repository.ImportJobRepository;
import com.mip.importjob.repository.ImportJobStepRepository;
import com.mip.notification.service.NotificationService;
import com.mip.plant.entity.Plant;
import com.mip.plant.service.PlantService;
import com.mip.record.entity.SourceDocument;
import com.mip.record.repository.SourceDocumentRepository;
import com.mip.security.MipUserDetails;
import com.mip.user.entity.User;
import com.mip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final ImportJobRepository jobRepository;
    private final ImportJobStepRepository stepRepository;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final ImportPipelineService pipelineService;
    private final PlantService plantService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ImportProperties importProperties;

    /**
     * Stores the file, records the source document and job, then runs the pipeline.
     * The job survives a pipeline crash (marked FAILED) so it can be inspected and re-run.
     */
    public ImportSummaryResponse uploadFile(Long plantId, MultipartFile file, MipUserDetails principal) {
        Long jobId = createJob(plantId, file, principal);
        runPipeline(jobId);
        notificationService.onValidationPending(jobId);
        ImportJob job = jobRepository.findById(jobId).orElseThrow();
        return new ImportSummaryResponse(job.getId(), job.getStatus().name(), job.getTotalRows(),
                job.getAutoImportedCount(), job.getNeedsValidationCount(),
                job.getRejectedCount(), job.getInvalidCount());
    }

    @Transactional
    public Long createJob(Long plantId, MultipartFile file, MipUserDetails principal) {
        Plant plant = plantService.requireAccessiblePlant(plantId, principal);
        validateUpload(file);

        byte[] content = readBytes(file);
        String checksum = sha256(content);
        if (sourceDocumentRepository.findByPlantIdAndChecksum(plantId, checksum).isPresent()) {
            throw new DuplicateResourceException(
                    "An identical file has already been imported for this plant");
        }

        Path storedPath = storeFile(plantId, file.getOriginalFilename(), content);
        User uploader = userService.getUser(principal.getId());
        SourceDocument document = sourceDocumentRepository.save(new SourceDocument(plant,
                sanitizeFilename(file.getOriginalFilename()),
                file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                content.length, checksum, storedPath.toString(), uploader));
        ImportJob job = jobRepository.save(new ImportJob(plant, document, uploader));
        log.info("Import job {} created for {} by user {}", job.getId(), document.getFilename(),
                uploader.getId());
        return job.getId();
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getJob(Long jobId, MipUserDetails principal) {
        ImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job", jobId));
        plantService.requireAccessiblePlant(job.getPlant().getId(), principal);
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getLatestJob(Long plantId, MipUserDetails principal) {
        plantService.requireAccessiblePlant(plantId, principal);
        ImportJob job = jobRepository.findTopByPlantIdOrderByCreatedAtDesc(plantId)
                .orElseThrow(() -> new ResourceNotFoundException("No import jobs exist for plant " + plantId));
        return toResponse(job);
    }

    /** Re-runs a FAILED job. A failed pipeline rolled back atomically, so a re-run starts clean. */
    public ImportJobResponse rerunJob(Long jobId, MipUserDetails principal) {
        prepareRerun(jobId, principal);
        runPipeline(jobId);
        notificationService.onValidationPending(jobId);
        return getJob(jobId, principal);
    }

    @Transactional
    public void prepareRerun(Long jobId, MipUserDetails principal) {
        ImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job", jobId));
        plantService.requireAccessiblePlant(job.getPlant().getId(), principal);
        if (job.getStatus() != ImportJobStatus.FAILED) {
            throw new BusinessRuleViolationException(
                    "Only failed jobs can be re-run; this job is " + job.getStatus());
        }
        job.setStatus(ImportJobStatus.PENDING);
        job.setErrorMessage(null);
        job.setStartedAt(null);
        job.setFinishedAt(null);
    }

    private void runPipeline(Long jobId) {
        try {
            pipelineService.execute(jobId);
        } catch (RuntimeException ex) {
            log.error("Import job {} failed", jobId, ex);
            pipelineService.markFailed(jobId, ex.getMessage());
            throw ex instanceof FileProcessingException ? ex
                    : new FileProcessingException("Import failed: " + ex.getMessage());
        }
    }

    private ImportJobResponse toResponse(ImportJob job) {
        var steps = stepRepository.findByJobIdOrderByOrderIndexAsc(job.getId()).stream()
                .map(s -> new ImportStepResponse(s.getName().name(), s.getStatus().name(),
                        s.getProcessedCount(), s.getMessage(), s.getStartedAt(), s.getFinishedAt()))
                .toList();
        return new ImportJobResponse(job.getId(), job.getPlant().getId(),
                job.getSourceDocument().getId(), job.getSourceDocument().getFilename(),
                job.getStatus().name(), job.getTotalRows(), job.getAutoImportedCount(),
                job.getNeedsValidationCount(), job.getRejectedCount(), job.getInvalidCount(),
                job.getErrorMessage(), job.getStartedAt(), job.getFinishedAt(), steps);
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Upload a non-empty .csv or .xlsx file");
        }
        long maxBytes = (long) importProperties.maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new PayloadTooLargeException(
                    "File exceeds the maximum size of " + importProperties.maxFileSizeMb() + " MB");
        }
        String name = file.getOriginalFilename() == null ? "" :
                file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv") && !name.endsWith(".xlsx")) {
            throw new InvalidRequestException("Only .csv and .xlsx files are supported");
        }
    }

    private Path storeFile(Long plantId, String originalName, byte[] content) {
        try {
            Path dir = Path.of(importProperties.storageDir(), "plant-" + plantId);
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + "-" + sanitizeFilename(originalName));
            Files.write(target, content);
            return target;
        } catch (IOException ex) {
            throw new FileProcessingException("File could not be stored: " + ex.getMessage());
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new FileProcessingException("Uploaded file could not be read: " + ex.getMessage());
        }
    }

    private String sanitizeFilename(String name) {
        String base = name == null ? "upload" : Path.of(name).getFileName().toString();
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
