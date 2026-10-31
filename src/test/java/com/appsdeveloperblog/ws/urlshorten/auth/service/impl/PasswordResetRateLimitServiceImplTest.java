package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.ExceedRateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class PasswordResetRateLimitServiceImplTest {
  private static final String EMAIL = "user@example.com";
  private static final String EMAIL_KEY =
      "password-reset:v1:rate-limit:email:"
          + "b4c9a289323b21a01c3e940f150eb9b8c542587f1abfd8f0e1cc1ffc5e475514";
  private static final String IP = "192.0.2.10";
  private static final String IP_KEY = "password-reset:v1:rate-limit:ip:" + IP;
  private StringRedisTemplate redis;
  private ValueOperations<String, String> values;
  private PasswordResetRateLimitServiceImpl service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redis = mock(StringRedisTemplate.class);
    values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    service = new PasswordResetRateLimitServiceImpl(redis);
    ReflectionTestUtils.setField(service, "emailLimit", 3);
    ReflectionTestUtils.setField(service, "ipLimit", 10);
    ReflectionTestUtils.setField(service, "emailWindowHours", 1);
    ReflectionTestUtils.setField(service, "ipWindowMinutes", 60);
  }

  @Test
  void firstRequestUsesHashedEmailKeyAndCreatesBothExpiryWindows() {
    when(values.increment(EMAIL_KEY)).thenReturn(1L);
    when(values.increment(IP_KEY)).thenReturn(1L);

    service.checkAndRecord(request(IP), EMAIL);

    verify(values).get(EMAIL_KEY);
    verify(values).increment(EMAIL_KEY);
    verify(values).increment(IP_KEY);
    verify(redis).expire(EMAIL_KEY, Duration.ofHours(1));
    verify(redis).expire(IP_KEY, Duration.ofMinutes(60));
  }

  @Test
  void laterRequestsBelowBothLimitsDoNotExtendExpiryWindows() {
    when(values.get(EMAIL_KEY)).thenReturn("2");
    when(values.get(IP_KEY)).thenReturn("9");
    when(values.increment(EMAIL_KEY)).thenReturn(3L);
    when(values.increment(IP_KEY)).thenReturn(10L);

    service.checkAndRecord(request(IP), EMAIL);

    verify(values).increment(EMAIL_KEY);
    verify(values).increment(IP_KEY);
    verify(redis, never()).expire(any(String.class), any(Duration.class));
  }

  @Test
  void emailAtLimitRejectsRequestBeforeUpdatingEitherCounter() {
    when(values.get(EMAIL_KEY)).thenReturn("3");

    assertThrows(ExceedRateLimitException.class, () -> service.checkAndRecord(request(IP), EMAIL));

    verify(values, never()).get(IP_KEY);
    verify(values, never()).increment(any(String.class));
    verify(redis, never()).expire(any(String.class), any(Duration.class));
  }

  @Test
  void ipAtLimitDoesNotConsumeRemainingEmailAllowance() {
    when(values.get(EMAIL_KEY)).thenReturn("2");
    when(values.get(IP_KEY)).thenReturn("10");

    assertThrows(ExceedRateLimitException.class, () -> service.checkAndRecord(request(IP), EMAIL));

    verify(values, never()).increment(any(String.class));
    verify(redis, never()).expire(any(String.class), any(Duration.class));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void missingRemoteAddressStillRecordsTheEmailLimit(String ip) {
    service.checkAndRecord(request(ip), EMAIL);
    verify(values).increment(EMAIL_KEY);
    verify(values, never()).get(startsWith("password-reset:v1:rate-limit:ip:"));
    verify(values, never()).increment(startsWith("password-reset:v1:rate-limit:ip:"));
  }

  @Test
  void absentHttpRequestStillRecordsTheEmailLimit() {
    service.checkAndRecord(null, EMAIL);
    verify(values).increment(EMAIL_KEY);
    verify(values, never()).get(startsWith("password-reset:v1:rate-limit:ip:"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void blankRedisCountsAreTreatedAsZero(String currentCount) {
    when(values.get(EMAIL_KEY)).thenReturn(currentCount);
    when(values.get(IP_KEY)).thenReturn(currentCount);

    service.checkAndRecord(request(IP), EMAIL);

    verify(values).increment(EMAIL_KEY);
    verify(values).increment(IP_KEY);
  }

  @Test
  void nullRedisIncrementsDoNotCreateExpiryWindows() {
    when(values.increment(EMAIL_KEY)).thenReturn(null);
    when(values.increment(IP_KEY)).thenReturn(null);

    service.checkAndRecord(request(IP), EMAIL);

    verify(redis, never()).expire(any(String.class), any(Duration.class));
  }

  private HttpServletRequest request(String ip) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn(ip);
    return request;
  }
}
