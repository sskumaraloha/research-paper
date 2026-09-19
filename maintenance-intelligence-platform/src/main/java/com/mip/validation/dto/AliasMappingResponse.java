package com.mip.validation.dto;

public record AliasMappingResponse(
        Long suggestionId,
        Long machineId,
        String alias,
        int revalidatedItemCount
) {
}
