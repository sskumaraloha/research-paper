package com.mip.entry.dto;

import java.util.List;

public record EntryConversationResponse(
        Long id,
        Long plantId,
        String status,
        EntryDraftResponse draft,
        List<EntryMessageResponse> messages,
        Long resultingRecordId
) {
}
