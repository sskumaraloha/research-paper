package com.mip.entry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntryMessageRequest(
        @NotBlank @Size(max = 1000) String message
) {
}
