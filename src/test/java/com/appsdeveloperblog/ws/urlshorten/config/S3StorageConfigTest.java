package com.appsdeveloperblog.ws.urlshorten.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import tools.jackson.databind.json.JsonMapper;

class S3StorageConfigTest {

  private static final String BUCKET_NAME = "existing-bucket";

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(S3StorageConfig.class)
          .withPropertyValues(
              "app.s3.endpoint=http://localhost:9000",
              "app.s3.access-key=test-access-key",
              "app.s3.secret-key=test-secret-key",
              "app.s3.bucket-name=" + BUCKET_NAME,
              "app.s3.region=us-east-1");

  private S3StorageConfig config;
  private S3Client s3Client;

  @BeforeEach
  void setUp() {
    config = new S3StorageConfig();
    s3Client = mock(S3Client.class);
    ReflectionTestUtils.setField(config, "bucketName", BUCKET_NAME);
  }

  @Test
  void existingBucketIsCheckedWithoutChangingItsPolicy() throws Exception {
    runInitializer();

    verifyOnlyHeadBucket();
  }

  @Test
  void missingBucketIsCreatedWhenHeadBucketReturns404() throws Exception {
    when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(s3Failure(404));

    runInitializer();

    var calls = inOrder(s3Client);
    calls.verify(s3Client).headBucket(headBucketRequest());
    calls.verify(s3Client).createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    verifyNoMoreInteractions(s3Client);
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 403, 500, 503})
  void non404ErrorsStopInitializationWithoutCreatingBucketOrChangingPolicy(int statusCode) {
    ReflectionTestUtils.setField(config, "publicRead", true);
    S3Exception failure = s3Failure(statusCode);
    when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(failure);

    IllegalStateException thrown = assertThrows(IllegalStateException.class, this::runInitializer);

    assertAll(
        () -> assertSame(failure, thrown.getCause()),
        () -> assertTrue(thrown.getMessage().contains(BUCKET_NAME)),
        () -> assertTrue(thrown.getMessage().contains(Integer.toString(statusCode))));
    verifyOnlyHeadBucket();
  }

  @Test
  void connectionFailureStopsInitializationWithoutCreatingBucketOrChangingPolicy() {
    ReflectionTestUtils.setField(config, "publicRead", true);
    SdkClientException failure = SdkClientException.create("Connection refused");
    when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(failure);

    assertSame(failure, assertThrows(SdkClientException.class, this::runInitializer));

    verifyOnlyHeadBucket();
  }

  @Test
  void publicReadPolicyIsAppliedToAnExistingBucketWhenEnabled() throws Exception {
    ReflectionTestUtils.setField(config, "publicRead", true);

    runInitializer();

    verify(s3Client).headBucket(headBucketRequest());
    verifyPublicReadPolicy();
    verifyNoMoreInteractions(s3Client);
  }

  @Test
  void publicReadPolicyIsAppliedAfterCreatingAMissingBucket() throws Exception {
    ReflectionTestUtils.setField(config, "publicRead", true);
    when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(s3Failure(404));

    runInitializer();

    var calls = inOrder(s3Client);
    calls.verify(s3Client).headBucket(headBucketRequest());
    calls.verify(s3Client).createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    calls.verify(s3Client).putBucketPolicy(any(PutBucketPolicyRequest.class));
    verifyPublicReadPolicy();
    verifyNoMoreInteractions(s3Client);
  }

  @Test
  void bucketCreationFailureIsPropagatedWithoutApplyingPublicReadPolicy() {
    ReflectionTestUtils.setField(config, "publicRead", true);
    when(s3Client.headBucket(any(HeadBucketRequest.class))).thenThrow(s3Failure(404));
    S3Exception failure = s3Failure(503);
    when(s3Client.createBucket(any(CreateBucketRequest.class))).thenThrow(failure);

    assertSame(failure, assertThrows(S3Exception.class, this::runInitializer));

    verify(s3Client).headBucket(headBucketRequest());
    verify(s3Client).createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    verifyNoMoreInteractions(s3Client);
  }

  @Test
  void publicReadPolicyFailureIsPropagated() {
    ReflectionTestUtils.setField(config, "publicRead", true);
    S3Exception failure = s3Failure(403);
    when(s3Client.putBucketPolicy(any(PutBucketPolicyRequest.class))).thenThrow(failure);

    assertSame(failure, assertThrows(S3Exception.class, this::runInitializer));

    verify(s3Client).headBucket(headBucketRequest());
    verify(s3Client).putBucketPolicy(any(PutBucketPolicyRequest.class));
    verifyNoMoreInteractions(s3Client);
  }

  @ParameterizedTest
  @CsvSource({
    "local, true, true",
    "local, false, false",
    "prod, true, false",
    "prod, false, false"
  })
  void initializerIsRegisteredOnlyForLocalProfileWithInitializationEnabled(
      String profile, boolean initializationEnabled, boolean initializerExpected) {
    contextRunner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
        .withPropertyValues("app.s3.initialize-bucket=" + initializationEnabled)
        .run(
            context -> {
              assertThat(context).hasNotFailed().hasSingleBean(S3Client.class);
              if (initializerExpected) {
                assertThat(context).hasSingleBean(ApplicationRunner.class);
              } else {
                assertThat(context).doesNotHaveBean(ApplicationRunner.class);
              }
            });
  }

  @Test
  void initializerIsNotRegisteredWhenInitializationPropertyIsMissing() {
    contextRunner
        .withInitializer(context -> context.getEnvironment().setActiveProfiles("local"))
        .run(
            context ->
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(S3Client.class)
                    .doesNotHaveBean(ApplicationRunner.class));
  }

  private void runInitializer() throws Exception {
    config.s3BucketInitializer(s3Client).run(new DefaultApplicationArguments(new String[0]));
  }

  private HeadBucketRequest headBucketRequest() {
    return HeadBucketRequest.builder().bucket(BUCKET_NAME).build();
  }

  private S3Exception s3Failure(int statusCode) {
    // The SDK builder declares AwsServiceException but creates an S3Exception.
    return (S3Exception) S3Exception.builder().statusCode(statusCode).build();
  }

  private void verifyPublicReadPolicy() {
    var request = ArgumentCaptor.forClass(PutBucketPolicyRequest.class);
    verify(s3Client).putBucketPolicy(request.capture());
    var policy = JsonMapper.builder().build().readTree(request.getValue().policy());
    var statement = policy.get("Statement").get(0);

    assertAll(
        () -> assertEquals(BUCKET_NAME, request.getValue().bucket()),
        () -> assertEquals(1, policy.get("Statement").size()),
        () -> assertEquals("Allow", statement.get("Effect").asString()),
        () -> assertEquals("*", statement.get("Principal").asString()),
        () -> assertEquals(1, statement.get("Action").size()),
        () -> assertEquals("s3:GetObject", statement.get("Action").get(0).asString()),
        () ->
            assertEquals(
                "arn:aws:s3:::" + BUCKET_NAME + "/*", statement.get("Resource").get(0).asString()));
  }

  private void verifyOnlyHeadBucket() {
    verify(s3Client).headBucket(headBucketRequest());
    verifyNoMoreInteractions(s3Client);
  }
}
