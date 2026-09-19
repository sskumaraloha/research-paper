package com.mip.importjob.entity;

public enum StagedRowStatus {
    /** Parsed, not yet routed. */
    PENDING,
    /** Row could not yield a record at all (missing mandatory data). */
    INVALID,
    /** Confidence at/above the plant auto-approve threshold: record created directly. */
    AUTO_IMPORTED,
    /** Routed to the human validation queue. */
    NEEDS_VALIDATION,
    /** Confidence below the plant low-confidence threshold: dropped without review. */
    REJECTED_LOW_CONFIDENCE,
    /** Approved from the validation queue and turned into a record. */
    IMPORTED_AFTER_VALIDATION,
    /** Rejected by a human from the validation queue. */
    REJECTED_BY_VALIDATOR
}
