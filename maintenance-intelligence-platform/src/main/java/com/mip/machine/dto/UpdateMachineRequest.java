package com.mip.machine.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * All fields optional; only supplied fields change. The machine code is deliberately
 * immutable: aliases, imports and history key off it.
 */
public record UpdateMachineRequest(
        @Size(max = 150) String name,
        Long lineId,
        @Size(max = 100) String manufacturer,
        @Size(max = 100) String model,
        String criticality,
        @PastOrPresent LocalDate commissionedOn,
        Boolean active
) {
}
