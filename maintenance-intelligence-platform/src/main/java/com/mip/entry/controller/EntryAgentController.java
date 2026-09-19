package com.mip.entry.controller;

import com.mip.entry.dto.EntryConversationResponse;
import com.mip.entry.dto.EntryMessageRequest;
import com.mip.entry.dto.StartConversationRequest;
import com.mip.entry.service.EntryAgentService;
import com.mip.security.MipUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/entry/conversations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
public class EntryAgentController {

    private final EntryAgentService entryAgentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryConversationResponse startConversation(
            @Valid @RequestBody StartConversationRequest request,
            @AuthenticationPrincipal MipUserDetails principal) {
        return entryAgentService.startConversation(request, principal);
    }

    @GetMapping("/{conversationId}")
    public EntryConversationResponse getConversation(@PathVariable Long conversationId,
                                                     @AuthenticationPrincipal MipUserDetails principal) {
        return entryAgentService.getConversation(conversationId, principal);
    }

    @PostMapping("/{conversationId}/messages")
    public EntryConversationResponse sendMessage(@PathVariable Long conversationId,
                                                 @Valid @RequestBody EntryMessageRequest request,
                                                 @AuthenticationPrincipal MipUserDetails principal) {
        return entryAgentService.handleMessage(conversationId, request, principal);
    }

    @PostMapping("/{conversationId}/confirm")
    public EntryConversationResponse confirm(@PathVariable Long conversationId,
                                             @AuthenticationPrincipal MipUserDetails principal) {
        return entryAgentService.confirm(conversationId, principal);
    }

    @PostMapping("/{conversationId}/request-edit")
    public EntryConversationResponse requestEdit(@PathVariable Long conversationId,
                                                 @AuthenticationPrincipal MipUserDetails principal) {
        return entryAgentService.requestEdit(conversationId, principal);
    }
}
