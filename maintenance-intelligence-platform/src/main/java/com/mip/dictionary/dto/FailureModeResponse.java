package com.mip.dictionary.dto;

import java.util.List;

public record FailureModeResponse(
        Long id,
        String code,
        String name,
        String category,
        List<String> keywords
) {
}
