package com.mip.plant.dto;

public record PlantSummaryResponse(
        Long id,
        String code,
        String name,
        String location
) {
}
