package com.mip.assistant.dto;

import com.mip.record.dto.RecordRowResponse;

import java.util.List;

public record AssistantAnswerResponse(
        String intent,
        String title,
        List<StatTile> tiles,
        List<AnswerSection> sections,
        List<RecordRowResponse> records,
        List<String> followUps
) {
}
