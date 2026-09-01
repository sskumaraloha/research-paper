package com.store.app.common.storage;

import com.store.app.exception.FileStorageException;
import com.store.app.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploads on the local filesystem under the configured base
 * directory; files are served back via the {@code /uploads/**} resource
 * handler. Active when {@code app.storage.provider=local} (the default).
 * <p>
 * Stored filenames are random UUIDs with a whitelisted extension, so no
 * user-supplied name or path ever reaches the filesystem.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final StorageProperties properties;
    private final Path basePath;

    public LocalFileStorageService(StorageProperties properties) {
        this.properties = properties;
        this.basePath = Paths.get(properties.localBaseDir()).toAbsolutePath().normalize();
    }

    @Override
    public String storeImage(MultipartFile file, String subdirectory) {
        String extension = validateImage(file);
        String filename = UUID.randomUUID() + "." + extension;

        Path targetDir = basePath.resolve(subdirectory).normalize();
        if (!targetDir.startsWith(basePath)) {
            throw new InvalidFileException("Invalid storage subdirectory");
        }

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file: " + ex.getMessage());
        }

        return properties.urlPrefix() + "/" + subdirectory + "/" + filename;
    }

    @Override
    public void delete(String url) {
        String prefix = properties.urlPrefix() + "/";
        if (url == null || !url.startsWith(prefix)) {
            return;
        }

        Path target = basePath.resolve(url.substring(prefix.length())).normalize();
        if (!target.startsWith(basePath)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            // Deleting a stored file is best-effort cleanup; the DB row is gone.
            log.warn("Could not delete stored file {}: {}", target, ex.getMessage());
        }
    }

    /** Returns the target extension for a valid image upload. */
    private String validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please select a file to upload");
        }

        long maxBytes = properties.maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new InvalidFileException(
                    "File is too large. Maximum size is " + properties.maxFileSizeMb() + " MB");
        }

        String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new InvalidFileException(
                    "Unsupported file type. Allowed types: JPEG, PNG, WebP, GIF");
        }
        return extension;
    }
}
