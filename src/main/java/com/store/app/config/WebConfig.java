package com.store.app.config;

import com.store.app.common.storage.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Serves locally stored uploads (product images etc.) at the configured
 * public URL prefix.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final StorageProperties storageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(storageProperties.localBaseDir())
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(storageProperties.urlPrefix() + "/**")
                .addResourceLocations(location);
    }
}
