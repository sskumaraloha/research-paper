package com.mip.machine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddAliasRequest(
        @NotBlank @Size(max = 150) String alias
) {
}
