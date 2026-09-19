package com.mip.validation.dto;

public record AliasSuggestionResponse(
        Long id,
        String rawText,
        int occurrences,
        Long suggestedMachineId,
        String suggestedMachineName,
        Double confidence,
        String status
) {
}
