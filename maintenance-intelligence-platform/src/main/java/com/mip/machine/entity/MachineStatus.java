package com.mip.machine.entity;

/**
 * Derived, never stored: computed from the machine's most recent maintenance record.
 */
public enum MachineStatus {
    /** No open downtime signal in the recent window. */
    RUNNING,
    /** Latest record is recent and reported significant downtime. */
    ATTENTION,
    /** No maintenance history at all. */
    UNKNOWN
}
