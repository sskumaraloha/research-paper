package com.mip.entry.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartConversationRequest(
        @NotNull Long plantId,
        /** Optional first message, e.g. "CNC-01 bearing seized this morning, down 2 hours". */
        @Size(max = 1000) String message
) {
}
