package com.appsdeveloperblog.ws.urlshorten.storage.service.impl;

import com.appsdeveloperblog.ws.urlshorten.config.S3StorageConfig;
import com.appsdeveloperblog.ws.urlshorten.storage.service.FileStorageService;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileStorageServiceImpl implements FileStorageService {

  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
  private static final List<String> ALLOWED_CONTENT_TYPES =
      Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif");

  private final S3Client s3Client;
  private final S3StorageConfig s3StorageConfig;

  @Override
  public String uploadFile(MultipartFile file, String folder) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File must not be empty.");
    }

    if (file.getSize() > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File size exceeds 5MB limit.");
    }

    String contentType = file.getContentType();
    if (contentType == null
        || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException("Only image files (PNG, JPG, WebP, GIF) are allowed.");
    }

    String originalFilename = file.getOriginalFilename();
    String extension = extractExtension(originalFilename);
    String sanitizedFolder =
        (folder != null && !folder.isBlank()) ? folder.trim().replaceAll("^/+|/+$", "") : "uploads";
    String key = sanitizedFolder + "/" + UUID.randomUUID() + extension;

    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(s3StorageConfig.getBucketName())
              .key(key)
              .contentType(contentType)
              .build();

      s3Client.putObject(
          putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
      log.info(
          "Uploaded file successfully to S3 bucket '{}' with key '{}'.",
          s3StorageConfig.getBucketName(),
          key);

      String endpoint = s3StorageConfig.getEndpoint().replaceAll("/+$", "");
      return endpoint + "/" + s3StorageConfig.getBucketName() + "/" + key;
    } catch (IOException e) {
      log.error("Failed to read file stream for S3 upload: {}", e.getMessage(), e);
      throw new IllegalStateException("Failed to upload image file: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteFile(String fileUrl) {
    if (fileUrl == null || fileUrl.isBlank()) {
      return;
    }

    try {
      String bucketName = s3StorageConfig.getBucketName();
      String prefix = s3StorageConfig.getEndpoint().replaceAll("/+$", "") + "/" + bucketName + "/";

      String key;
      if (fileUrl.startsWith(prefix)) {
        key = fileUrl.substring(prefix.length());
      } else if (fileUrl.contains("/" + bucketName + "/")) {
        key = fileUrl.substring(fileUrl.indexOf("/" + bucketName + "/") + bucketName.length() + 2);
      } else {
        key = fileUrl;
      }

      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(bucketName).key(key).build();

      s3Client.deleteObject(deleteObjectRequest);
      log.info("Deleted object '{}' from S3 bucket '{}'.", key, bucketName);
    } catch (Exception e) {
      log.warn("Failed to delete S3 file '{}': {}", fileUrl, e.getMessage());
    }
  }

  private String extractExtension(String filename) {
    if (filename != null && filename.contains(".")) {
      return filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }
    return ".png";
  }
}
