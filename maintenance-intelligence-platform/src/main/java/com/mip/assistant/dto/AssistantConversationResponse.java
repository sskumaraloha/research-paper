package com.mip.assistant.dto;

import java.time.Instant;
import java.util.List;

public record AssistantConversationResponse(
        Long id,
        Long plantId,
        String title,
        Instant updatedAt,
        /** Present on the detail endpoint; null in listings. */
        List<AssistantMessageResponse> messages
) {
    public record AssistantMessageResponse(String sender, String content, String intent,
                                           Instant createdAt) {
    }
}
