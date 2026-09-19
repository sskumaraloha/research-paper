package com.mip.importjob.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.import")
public record ImportProperties(
        int maxFileSizeMb,
        String storageDir
) {
}
