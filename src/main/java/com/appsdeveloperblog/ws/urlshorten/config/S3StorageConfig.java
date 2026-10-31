package com.appsdeveloperblog.ws.urlshorten.config;

import java.net.URI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

@Slf4j
@Configuration
public class S3StorageConfig {

  @Getter
  @Value("${app.s3.endpoint:http://localhost:9000}")
  private String endpoint;

  @Value("${app.s3.access-key:minioadmin}")
  private String accessKey;

  @Value("${app.s3.secret-key:miniopassword123}")
  private String secretKey;

  @Getter
  @Value("${app.s3.bucket-name:url-shortener-avatars}")
  private String bucketName;

  @Value("${app.s3.region:us-east-1}")
  private String region;

  @Bean
  public S3Client s3Client() {
    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .region(Region.of(region))
        .forcePathStyle(true) // Required for MinIO S3-compatible path-style access
        .build();
  }

  @Bean
  public ApplicationRunner s3BucketInitializer(S3Client s3Client) {
    return args -> {
      try {
        boolean bucketExists = true;
        try {
          s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (NoSuchBucketException ex) {
          bucketExists = false;
        } catch (Exception ex) {
          // If HeadBucket throws S3Exception with 404
          bucketExists = false;
        }

        if (!bucketExists) {
          s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
          log.info("S3 Bucket '{}' created successfully on MinIO.", bucketName);
        }

        // Set anonymous read policy so browsers can directly view avatar images
        String readOnlyPolicy =
            """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """
                .formatted(bucketName);

        try {
          s3Client.putBucketPolicy(
              PutBucketPolicyRequest.builder().bucket(bucketName).policy(readOnlyPolicy).build());
          log.info("Public read policy applied to bucket '{}'.", bucketName);
        } catch (Exception e) {
          log.warn("Could not apply bucket policy to '{}': {}", bucketName, e.getMessage());
        }
      } catch (Exception e) {
        log.warn("MinIO / S3 initialization skipped or unavailable: {}", e.getMessage());
      }
    };
  }
}
