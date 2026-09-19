package com.mip.entry.controller;

import com.mip.entry.config.WhatsAppProperties;
import com.mip.entry.dto.WhatsAppInboundRequest;
import com.mip.entry.dto.WhatsAppReplyResponse;
import com.mip.entry.service.EntryAgentService;
import com.mip.exception.ResourceNotFoundException;
import com.mip.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Provider-agnostic inbound endpoint for a WhatsApp gateway. Authenticated by a shared
 * secret in X-Webhook-Token, never by a user session; the sender is identified by the
 * phone number in the payload. Disabled (404) unless app.whatsapp.webhook-token is set.
 */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final EntryAgentService entryAgentService;
    private final WhatsAppProperties properties;

    @PostMapping
    public WhatsAppReplyResponse inbound(
            @RequestHeader(value = "X-Webhook-Token", required = false) String token,
            @Valid @RequestBody WhatsAppInboundRequest request) {
        if (!properties.enabled()) {
            throw new ResourceNotFoundException("WhatsApp webhook is not enabled");
        }
        if (token == null || !MessageDigest.isEqual(
                properties.webhookToken().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Invalid webhook token");
        }
        return entryAgentService.handleWhatsAppInbound(request.from(), request.text());
    }
}
