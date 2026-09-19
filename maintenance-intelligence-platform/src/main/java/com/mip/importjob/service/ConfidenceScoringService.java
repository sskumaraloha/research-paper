package com.mip.importjob.service;

import org.springframework.stereotype.Service;

/**
 * Combines per-signal confidences into the row score that routing thresholds act on.
 * Machine identity dominates: a record on the wrong machine is worse than a record
 * with an unknown failure mode.
 */
@Service
public class ConfidenceScoringService {

    private static final double WEIGHT_MACHINE = 0.50;
    private static final double WEIGHT_FAILURE_MODE = 0.20;
    private static final double WEIGHT_DATE = 0.15;
    private static final double WEIGHT_DOWNTIME = 0.10;
    private static final double WEIGHT_DESCRIPTION = 0.05;

    public double score(Double machineConfidence, Double failureModeConfidence, NormalizedRow row) {
        double score = 0;
        score += WEIGHT_MACHINE * (machineConfidence == null ? 0 : machineConfidence);
        score += WEIGHT_FAILURE_MODE * (failureModeConfidence == null ? 0 : failureModeConfidence);
        score += WEIGHT_DATE * (row.date() == null ? 0 : 1);
        score += WEIGHT_DOWNTIME * (row.downtimeMinutes() == null ? 0 : 1);
        score += WEIGHT_DESCRIPTION * (row.description() == null ? 0 : 1);
        return Math.round(score * 100.0) / 100.0;
    }
}
