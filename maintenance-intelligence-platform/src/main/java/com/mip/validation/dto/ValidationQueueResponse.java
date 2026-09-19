package com.mip.validation.dto;

import com.mip.common.dto.PageResponse;

public record ValidationQueueResponse(
        long pendingCount,
        PageResponse<ValidationItemResponse> items
) {
}
