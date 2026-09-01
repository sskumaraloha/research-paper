package com.store.app.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction for uploaded files. The local-disk implementation
 * is active by default; a cloud implementation (e.g. AWS S3) can be added
 * by implementing this interface and switching {@code app.storage.provider}.
 */
public interface FileStorageService {

    /**
     * Validates and stores an uploaded image, returning the public URL
     * it will be served from.
     *
     * @param file         the uploaded file
     * @param subdirectory logical grouping, e.g. {@code products}
     * @throws com.store.app.exception.InvalidFileException
     *         if the file is empty, too large, or not an allowed image type
     * @throws com.store.app.exception.FileStorageException
     *         if the file cannot be written
     */
    String storeImage(MultipartFile file, String subdirectory);

    /**
     * Deletes a previously stored file by its public URL. Unknown or
     * already-deleted URLs are ignored.
     */
    void delete(String url);
}
