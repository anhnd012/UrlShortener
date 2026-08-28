package com.appsdeveloperblog.ws.urlshorten.storage.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.config.S3StorageConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3FileStorageServiceImplTest {

  private S3Client s3Client;
  private S3StorageConfig s3StorageConfig;
  private S3FileStorageServiceImpl fileStorageService;

  @BeforeEach
  void setUp() {
    s3Client = mock(S3Client.class);
    s3StorageConfig = mock(S3StorageConfig.class);

    when(s3StorageConfig.getEndpoint()).thenReturn("http://localhost:9000");
    when(s3StorageConfig.getBucketName()).thenReturn("url-shortener-avatars");

    fileStorageService = new S3FileStorageServiceImpl(s3Client, s3StorageConfig);
  }

  @Test
  void uploadFileSuccessfully() {
    MockMultipartFile file =
        new MockMultipartFile("file", "test.png", "image/png", "image-content".getBytes());

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    String url = fileStorageService.uploadFile(file, "avatars");

    assertAll(
        () -> assertNotNull(url),
        () -> assertTrue(url.startsWith("http://localhost:9000/url-shortener-avatars/avatars/")),
        () -> assertTrue(url.endsWith(".png")));

    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void uploadFileThrowsWhenFileIsEmpty() {
    MockMultipartFile emptyFile =
        new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

    assertThrows(
        IllegalArgumentException.class,
        () -> fileStorageService.uploadFile(emptyFile, "avatars"));
  }

  @Test
  void uploadFileThrowsWhenInvalidContentType() {
    MockMultipartFile textFile =
        new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

    assertThrows(
        IllegalArgumentException.class,
        () -> fileStorageService.uploadFile(textFile, "avatars"));
  }

  @Test
  void deleteFileCallsS3ClientDeleteObject() {
    String fileUrl = "http://localhost:9000/url-shortener-avatars/avatars/123-avatar.png";

    fileStorageService.deleteFile(fileUrl);

    verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
  }
}
