package com.mip.insight.entity;

public enum InsightType {
    /** Same failure mode recurring on one machine within the window. */
    REPEATED_FAILURES,
    /** Machine downtime rising sharply versus the previous window. */
    RISING_DOWNTIME,
    /** One machine dominating the plant's downtime. */
    CHRONIC_TOP_MACHINE,
    /** One failure mode dominating the plant's downtime. */
    DOMINANT_FAILURE_MODE,
    /** The same spare part replaced repeatedly on one machine. */
    PART_WEAR
}
