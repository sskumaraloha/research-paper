package com.mip.entry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Provider-agnostic inbound message a WhatsApp gateway relays to us. */
public record WhatsAppInboundRequest(
        @NotBlank @Size(max = 20) String from,
        @NotBlank @Size(max = 1000) String text
) {
}
