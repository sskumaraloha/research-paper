package com.mip.assistant.service;

import com.mip.assistant.dto.AskRequest;
import com.mip.assistant.dto.AssistantAnswerResponse;
import com.mip.plant.service.PlantService;
import com.mip.security.MipUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private final IntentRouterService intentRouterService;
    private final AnswerBuilderService answerBuilderService;
    private final SuggestionService suggestionService;
    private final PlantService plantService;

    @Transactional(readOnly = true)
    public AssistantAnswerResponse ask(AskRequest request, MipUserDetails principal) {
        plantService.requireAccessiblePlant(request.plantId(), principal);
        RoutedIntent routed = intentRouterService.route(request.plantId(), request.question());
        log.debug("Assistant routed '{}' to {}", request.question(), routed.intent());

        return switch (routed.intent()) {
            case MACHINE_HISTORY -> answerBuilderService.machineHistory(routed.machine(), principal);
            case REPEATED_FAILURES -> answerBuilderService.repeatedFailures(request.plantId(),
                    routed.machine());
            case FAILURE_MODE_DOWNTIME -> answerBuilderService.failureModeDowntime(request.plantId(),
                    routed.failureMode());
            case PART_USAGE -> answerBuilderService.partUsage(request.plantId(), routed.part());
            case HIGHEST_DOWNTIME -> answerBuilderService.highestDowntime(request.plantId(), principal);
            case RECORD_SEARCH -> answerBuilderService.recordSearch(request.question(), principal);
            case SIMILAR_FAILURES -> answerBuilderService.similarFailures(request.plantId(),
                    routed.failureMode(), request.question(), principal);
            case PLANT_KPIS -> answerBuilderService.plantKpis(request.plantId(), principal);
            case FALLBACK -> answerBuilderService.fallback(
                    suggestionService.suggestions(request.plantId(), principal));
        };
    }
}
