package com.mip.assistant.dto;

import com.mip.record.dto.RecordRowResponse;

import java.util.List;

public record AssistantAnswerResponse(
        String intent,
        String title,
        List<StatTile> tiles,
        List<AnswerSection> sections,
        List<RecordRowResponse> records,
        List<String> followUps,
        Long conversationId
) {
    /** Builders create answers without a conversation; the service attaches it. */
    public AssistantAnswerResponse(String intent, String title, List<StatTile> tiles,
                                   List<AnswerSection> sections, List<RecordRowResponse> records,
                                   List<String> followUps) {
        this(intent, title, tiles, sections, records, followUps, null);
    }

    public AssistantAnswerResponse withConversationId(Long id) {
        return new AssistantAnswerResponse(intent, title, tiles, sections, records, followUps, id);
    }
}
