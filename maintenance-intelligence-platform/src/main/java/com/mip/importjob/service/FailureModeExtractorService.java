package com.mip.importjob.service;

import com.mip.dictionary.entity.FailureMode;
import com.mip.dictionary.service.FailureModeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Derives the failure mode of an import row. An explicit failure-mode column is
 * trusted more than a keyword hit inside the free-text description.
 */
@Service
@RequiredArgsConstructor
public class FailureModeExtractorService {

    public record ExtractionResult(FailureMode failureMode, double confidence) {
    }

    private final FailureModeService failureModeService;

    public Optional<ExtractionResult> extract(String failureModeText, String description) {
        if (failureModeText != null && !failureModeText.isBlank()) {
            Optional<FailureMode> explicit = failureModeService.resolveFailureMode(failureModeText);
            if (explicit.isPresent()) {
                return Optional.of(new ExtractionResult(explicit.get(), 0.9));
            }
        }
        if (description != null && !description.isBlank()) {
            return failureModeService.resolveFailureMode(description)
                    .map(mode -> new ExtractionResult(mode, 0.7));
        }
        return Optional.empty();
    }
}
