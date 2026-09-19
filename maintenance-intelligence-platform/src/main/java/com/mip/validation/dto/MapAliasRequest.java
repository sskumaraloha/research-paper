package com.mip.validation.dto;

import jakarta.validation.constraints.NotNull;

public record MapAliasRequest(
        @NotNull Long machineId
) {
}
