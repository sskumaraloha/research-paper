package com.mip.machine.service;

import com.mip.machine.entity.Machine;

/** Result of resolving free text to a machine, with how sure the resolver is and why. */
public record MachineResolution(
        Machine machine,
        double confidence,
        ResolutionMethod method
) {
    public enum ResolutionMethod {
        EXACT_CODE,
        EXACT_NAME,
        ALIAS,
        SYNONYM,
        FUZZY
    }
}
