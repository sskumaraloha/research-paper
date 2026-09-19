package com.mip.validation.dto;

public record ValidationDecisionResponse(
        Long itemId,
        String status,
        Long recordId
) {
}
