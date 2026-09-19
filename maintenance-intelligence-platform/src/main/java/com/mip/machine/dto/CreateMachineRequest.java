package com.mip.machine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateMachineRequest(
        @NotNull Long plantId,
        Long lineId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 100) String manufacturer,
        @Size(max = 100) String model,
        String criticality,
        @PastOrPresent LocalDate commissionedOn
) {
}
