package com.store.app.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * File storage settings bound from {@code app.storage.*}.
 *
 * @param provider       which FileStorageService implementation is active
 *                       ({@code local} now; e.g. {@code s3} once integrated)
 * @param localBaseDir   directory on disk where the local provider stores files
 * @param urlPrefix      public URL prefix the stored files are served under
 * @param maxFileSizeMb  maximum accepted upload size per file
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        @DefaultValue("local") String provider,
        @DefaultValue("uploads") String localBaseDir,
        @DefaultValue("/uploads") String urlPrefix,
        @DefaultValue("2") long maxFileSizeMb
) {
}
