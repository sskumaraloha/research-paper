package com.mip.validation.controller;

import com.mip.common.dto.CountResponse;
import com.mip.record.dto.RejectRequest;
import com.mip.security.MipUserDetails;
import com.mip.validation.dto.AliasMappingResponse;
import com.mip.validation.dto.AliasSuggestionResponse;
import com.mip.validation.dto.EditValidationItemRequest;
import com.mip.validation.dto.MapAliasRequest;
import com.mip.validation.dto.ValidationDecisionResponse;
import com.mip.validation.dto.ValidationQueueResponse;
import com.mip.validation.service.AliasSuggestionService;
import com.mip.validation.service.ValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/validation")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;
    private final AliasSuggestionService aliasSuggestionService;

    @GetMapping("/queue")
    public ValidationQueueResponse getQueue(@RequestParam Long plantId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @AuthenticationPrincipal MipUserDetails principal) {
        return validationService.getQueue(plantId, page, size, principal);
    }

    @GetMapping("/pending-count")
    public CountResponse pendingCount(@RequestParam Long plantId,
                                      @AuthenticationPrincipal MipUserDetails principal) {
        return new CountResponse(validationService.pendingCount(plantId, principal));
    }

    @PostMapping("/{itemId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ValidationDecisionResponse approve(@PathVariable Long itemId,
                                              @AuthenticationPrincipal MipUserDetails principal) {
        return validationService.approve(itemId, principal);
    }

    @PostMapping("/{itemId}/edit-approve")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ValidationDecisionResponse editAndApprove(@PathVariable Long itemId,
                                                     @Valid @RequestBody EditValidationItemRequest request,
                                                     @AuthenticationPrincipal MipUserDetails principal) {
        return validationService.editAndApprove(itemId, request, principal);
    }

    @PostMapping("/{itemId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ValidationDecisionResponse reject(@PathVariable Long itemId,
                                             @Valid @RequestBody RejectRequest request,
                                             @AuthenticationPrincipal MipUserDetails principal) {
        return validationService.reject(itemId, request, principal);
    }

    // --- alias suggestions (unresolved machine names from imports) ---

    @GetMapping("/alias-suggestions")
    public List<AliasSuggestionResponse> listAliasSuggestions(
            @RequestParam Long plantId,
            @AuthenticationPrincipal MipUserDetails principal) {
        return aliasSuggestionService.listPending(plantId, principal);
    }

    @PostMapping("/alias-suggestions/{suggestionId}/map")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public AliasMappingResponse mapAlias(@PathVariable Long suggestionId,
                                         @Valid @RequestBody MapAliasRequest request,
                                         @AuthenticationPrincipal MipUserDetails principal) {
        return aliasSuggestionService.mapAlias(suggestionId, request, principal);
    }
}
