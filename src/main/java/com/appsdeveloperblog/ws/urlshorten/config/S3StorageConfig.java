package com.appsdeveloperblog.ws.urlshorten.config;

import java.net.URI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Configuration
public class S3StorageConfig {

  @Getter
  @Value("${app.s3.endpoint}")
  private String endpoint;

  @Value("${app.s3.access-key}")
  private String accessKey;

  @Value("${app.s3.secret-key}")
  private String secretKey;

  @Getter
  @Value("${app.s3.bucket-name}")
  private String bucketName;

  @Value("${app.s3.region}")
  private String region;

  @Value("${app.s3.force-path-style:false}")
  private boolean forcePathStyle;

  @Value("${app.s3.public-read:false}")
  private boolean publicRead;

  @Bean
  public S3Client s3Client() {
    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .credentialsProvider(StaticCredentialsProvider.create(credentials))
        .region(Region.of(region))
        .forcePathStyle(forcePathStyle)
        .build();
  }

  @Bean
  @Profile("local")
  @ConditionalOnProperty(prefix = "app.s3", name = "initialize-bucket", havingValue = "true")
  public ApplicationRunner s3BucketInitializer(S3Client s3Client) {
    return args -> {
      try {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
      } catch (S3Exception ex) {
        if (ex.statusCode() != 404) {
          throw new IllegalStateException(
              "Cannot access local MinIO bucket '"
                  + bucketName
                  + "'. HTTP status: "
                  + ex.statusCode(),
              ex);
        }

        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        log.info("MinIO bucket '{}' created.", bucketName);
      }

      if (publicRead) {
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

        s3Client.putBucketPolicy(
            PutBucketPolicyRequest.builder().bucket(bucketName).policy(readOnlyPolicy).build());
        log.info("Anonymous read policy applied to local MinIO bucket '{}'.", bucketName);
      }

      log.info("Local MinIO bucket '{}' is ready.", bucketName);
    };
  }
}
