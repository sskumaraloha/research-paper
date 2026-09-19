package com.mip.common.timelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.time-limit")
public record TimeLimitProperties(
        /** Budget for one import-pipeline run; the job is marked FAILED when exceeded. */
        int importSeconds
) {
}
