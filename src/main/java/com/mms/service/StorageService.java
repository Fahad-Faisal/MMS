// src/main/java/com/mms/service/StorageService.java
package com.mms.service;

import com.mms.dto.response.UploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {
    private final RestTemplate restTemplate;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.bucket.photos}")
    private String photosBucket;

    @Value("${supabase.bucket.documents}")
    private String documentsBucket;

    @Value("${storage.max-file-size:10485760}")  // Default 10MB
    private long maxFileSize;

    private String currentStorageKey;

    public StorageService() {
        this.restTemplate = new RestTemplate();
    }

    public UploadResponse uploadImage(MultipartFile file) {
        log.info("Uploading image: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
        validateFile(file);

        String storageKey = generateStorageKey("images", file.getOriginalFilename());
        log.debug("Generated storage key: {}", storageKey);

        String url = uploadToSupabase(file, storageKey, photosBucket);
        log.info("Image uploaded successfully. Public URL: {}", url);

        return UploadResponse.builder()
                .url(url)
                .publicId(storageKey)
                .build();
    }

    public String uploadFile(MultipartFile file, String folder, String entityId) {
        log.info("Uploading file: {}, size: {} bytes, folder: {}/{}",
                file.getOriginalFilename(), file.getSize(), folder, entityId);
        validateFile(file);

        String storageKey = generateStorageKey(folder + "/" + entityId, file.getOriginalFilename());
        currentStorageKey = storageKey;
        log.debug("Generated storage key: {}", storageKey);

        String url = uploadToSupabase(file, storageKey, documentsBucket);
        log.info("File uploaded successfully. Public URL: {}", url);
        return url;
    }

    // src/main/java/com/mms/service/StorageService.java
// Update the validateFile method to log the actual max size

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.error("File is null or empty");
            throw new RuntimeException("File is required");
        }

        long fileSize = file.getSize();
        long maxSizeInBytes = maxFileSize; // This comes from application.yml

        log.debug("File validation - Size: {} bytes, Max allowed: {} bytes", fileSize, maxSizeInBytes);
        log.debug("File validation - Size: {} MB, Max allowed: {} MB",
                fileSize / 1024 / 1024, maxSizeInBytes / 1024 / 1024);

        if (fileSize > maxSizeInBytes) {
            String errorMsg = String.format(
                    "File exceeds maximum size of %d MB (current: %.2f MB)",
                    maxSizeInBytes / 1024 / 1024,
                    (double) fileSize / 1024 / 1024
            );
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        String contentType = file.getContentType();
        log.debug("File content type: {}", contentType);

        if (contentType == null || !isAllowedContentType(contentType)) {
            String errorMsg = String.format(
                    "File type not allowed: %s. Only images and PDFs are accepted",
                    contentType
            );
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        log.info("File validation passed: {}, size: {} bytes ({} MB)",
                file.getOriginalFilename(), fileSize, fileSize / 1024 / 1024);
    }

    private boolean isAllowedContentType(String contentType) {
        return contentType.startsWith("image/") || contentType.equals("application/pdf");
    }

    private String generateStorageKey(String folder, String filename) {
        String uuid = UUID.randomUUID().toString();
        String extension = "";
        if (filename != null && filename.contains(".")) {
            extension = filename.substring(filename.lastIndexOf("."));
        }
        // Sanitize filename to prevent path traversal
        String sanitizedFolder = folder.replaceAll("\\.\\./", "").replaceAll("/\\.\\.", "");
        return sanitizedFolder + "/" + uuid + extension;
    }

    private String uploadToSupabase(MultipartFile file, String storageKey, String bucket) {
        try {
            // Build the Supabase Storage API URL
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + storageKey;
            log.debug("Upload URL: {}", uploadUrl);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("Authorization", "Bearer " + serviceRoleKey);
            headers.set("x-upsert", "false");

            // Get file bytes
            byte[] fileBytes = file.getBytes();
            log.debug("File bytes length: {}", fileBytes.length);

            // Create request entity
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

            // Upload to Supabase
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.debug("Supabase upload response status: {}", response.getStatusCode());

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Supabase upload failed with status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to upload to Supabase: " + response.getStatusCode());
            }

            // Generate the public URL
            String publicUrl = generatePublicUrl(bucket, storageKey);
            log.debug("Generated public URL: {}", publicUrl);

            return publicUrl;

        } catch (IOException e) {
            log.error("Failed to read file bytes", e);
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to upload to Supabase", e);
            throw new RuntimeException("Failed to upload to Supabase: " + e.getMessage(), e);
        }
    }

    private String generatePublicUrl(String bucket, String storageKey) {
        return String.format("%s/storage/v1/object/public/%s/%s",
                supabaseUrl, bucket, storageKey);
    }

    public String getStorageKey() {
        return currentStorageKey;
    }

    public void deleteFile(String storageKey, String bucket) {
        try {
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + storageKey;
            log.info("Deleting file: {} from bucket: {}", storageKey, bucket);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + serviceRoleKey);

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    requestEntity,
                    Void.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully deleted file: {} from bucket: {}", storageKey, bucket);
            } else {
                log.warn("Failed to delete file: {} from bucket: {}. Status: {}",
                        storageKey, bucket, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to delete file from Supabase: {}", storageKey, e);
            throw new RuntimeException("Failed to delete file from Supabase", e);
        }
    }

    public String getFileUrl(String storageKey, String bucket) {
        return generatePublicUrl(bucket, storageKey);
    }
}