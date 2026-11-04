package com.appsdeveloperblog.ws.urlshorten.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

@EnabledIfSystemProperty(named = "s3.smoke", matches = "true")
@ActiveProfiles("prod")
@SpringBootTest(
    classes = S3StorageConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "app.s3.initialize-bucket=false")
class AwsS3ConnectionSmokeTest {

  @Autowired private S3Client s3Client;
  @Autowired private S3StorageConfig config;
  @Autowired private Environment environment;

  @Test
  void canAccessConfiguredAwsBucket() {
    assertEquals(environment.getRequiredProperty("S3_BUCKET"), config.getBucketName());
    assertEquals(
        "https://s3." + environment.getRequiredProperty("S3_REGION") + ".amazonaws.com",
        config.getEndpoint());

    var response =
        s3Client.headBucket(HeadBucketRequest.builder().bucket(config.getBucketName()).build());

    assertTrue(response.sdkHttpResponse().isSuccessful());
  }
}
