package com.mip.entry.dto;

import java.time.Instant;

public record EntryMessageResponse(
        String sender,
        String content,
        Instant createdAt
) {
}
