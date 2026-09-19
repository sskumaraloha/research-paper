package com.mip.entry.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.whatsapp")
public record WhatsAppProperties(
        /** Shared secret the gateway must send in X-Webhook-Token; blank disables the webhook. */
        String webhookToken
) {
    public boolean enabled() {
        return webhookToken != null && !webhookToken.isBlank();
    }
}
