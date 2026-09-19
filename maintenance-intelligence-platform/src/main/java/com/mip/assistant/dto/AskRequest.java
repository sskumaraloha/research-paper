package com.mip.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotNull Long plantId,
        @NotBlank @Size(max = 500) String question
) {
}
