package com.mip.entry.dto;

/** What the gateway should send back to the sender. */
public record WhatsAppReplyResponse(
        Long conversationId,
        String status,
        String reply
) {
}
