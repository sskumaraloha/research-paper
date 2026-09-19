package com.mip.assistant.controller;

import com.mip.assistant.dto.AskRequest;
import com.mip.assistant.dto.AssistantAnswerResponse;
import com.mip.assistant.dto.AssistantConversationResponse;
import com.mip.assistant.service.AssistantService;
import com.mip.assistant.service.SuggestionService;
import com.mip.security.MipUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final SuggestionService suggestionService;

    @PostMapping("/ask")
    public AssistantAnswerResponse ask(@Valid @RequestBody AskRequest request,
                                       @AuthenticationPrincipal MipUserDetails principal) {
        return assistantService.ask(request, principal);
    }

    @GetMapping("/suggestions")
    public List<String> suggestions(@RequestParam Long plantId,
                                    @AuthenticationPrincipal MipUserDetails principal) {
        return suggestionService.suggestions(plantId, principal);
    }

    @GetMapping("/conversations")
    public List<AssistantConversationResponse> listConversations(
            @AuthenticationPrincipal MipUserDetails principal) {
        return assistantService.listConversations(principal);
    }

    @GetMapping("/conversations/{conversationId}")
    public AssistantConversationResponse getConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal MipUserDetails principal) {
        return assistantService.getConversation(conversationId, principal);
    }
}
