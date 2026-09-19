package com.mip.assistant.service;

import com.mip.assistant.dto.AnswerSection;
import com.mip.assistant.dto.AskRequest;
import com.mip.assistant.dto.AssistantAnswerResponse;
import com.mip.assistant.dto.AssistantConversationResponse;
import com.mip.assistant.dto.StatTile;
import com.mip.assistant.entity.AssistantConversation;
import com.mip.assistant.entity.AssistantMessage;
import com.mip.assistant.repository.AssistantConversationRepository;
import com.mip.assistant.repository.AssistantMessageRepository;
import com.mip.exception.ResourceNotFoundException;
import com.mip.plant.entity.Plant;
import com.mip.plant.service.PlantService;
import com.mip.security.MipUserDetails;
import com.mip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantService {

    private final IntentRouterService intentRouterService;
    private final AnswerBuilderService answerBuilderService;
    private final SuggestionService suggestionService;
    private final PlantService plantService;
    private final UserService userService;
    private final AssistantConversationRepository conversationRepository;
    private final AssistantMessageRepository messageRepository;

    @Transactional
    public AssistantAnswerResponse ask(AskRequest request, MipUserDetails principal) {
        Plant plant = plantService.requireAccessiblePlant(request.plantId(), principal);
        RoutedIntent routed = intentRouterService.route(request.plantId(), request.question());
        log.debug("Assistant routed '{}' to {}", request.question(), routed.intent());

        AssistantAnswerResponse answer = switch (routed.intent()) {
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

        AssistantConversation conversation = resolveConversation(request, plant, principal);
        messageRepository.save(new AssistantMessage(conversation, AssistantMessage.Sender.USER,
                request.question().trim(), null));
        messageRepository.save(new AssistantMessage(conversation, AssistantMessage.Sender.ASSISTANT,
                flatten(answer), answer.intent()));
        return answer.withConversationId(conversation.getId());
    }

    @Transactional(readOnly = true)
    public List<AssistantConversationResponse> listConversations(MipUserDetails principal) {
        return conversationRepository.findTop20ByUserIdOrderByUpdatedAtDesc(principal.getId()).stream()
                .map(c -> new AssistantConversationResponse(c.getId(), c.getPlant().getId(),
                        c.getTitle(), c.getUpdatedAt(), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssistantConversationResponse getConversation(Long conversationId,
                                                         MipUserDetails principal) {
        AssistantConversation conversation = requireOwnConversation(conversationId, principal);
        List<AssistantConversationResponse.AssistantMessageResponse> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream()
                .map(m -> new AssistantConversationResponse.AssistantMessageResponse(
                        m.getSender().name(), m.getContent(), m.getIntent(), m.getCreatedAt()))
                .toList();
        return new AssistantConversationResponse(conversation.getId(), conversation.getPlant().getId(),
                conversation.getTitle(), conversation.getUpdatedAt(), messages);
    }

    private AssistantConversation resolveConversation(AskRequest request, Plant plant,
                                                      MipUserDetails principal) {
        if (request.conversationId() != null) {
            return requireOwnConversation(request.conversationId(), principal);
        }
        String title = request.question().trim();
        title = title.length() > 150 ? title.substring(0, 147) + "..." : title;
        return conversationRepository.save(new AssistantConversation(
                userService.getUser(principal.getId()), plant, title));
    }

    private AssistantConversation requireOwnConversation(Long conversationId,
                                                         MipUserDetails principal) {
        return conversationRepository.findByIdAndUserId(conversationId, principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", conversationId));
    }

    /** Text form of a structured answer, persisted as the assistant's message. */
    private String flatten(AssistantAnswerResponse answer) {
        StringBuilder text = new StringBuilder(answer.title());
        for (StatTile tile : answer.tiles()) {
            text.append("\n").append(tile.label()).append(": ").append(tile.value());
            if (tile.unit() != null) {
                text.append(" ").append(tile.unit());
            }
        }
        for (AnswerSection section : answer.sections()) {
            text.append("\n\n").append(section.heading()).append("\n").append(section.text());
        }
        String flat = text.toString();
        return flat.length() > 4000 ? flat.substring(0, 4000) : flat;
    }
}
