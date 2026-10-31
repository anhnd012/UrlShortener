package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.ExceedRateLimitException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignUpRequest;
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

class SignUpRateLimitServiceImplTest {
  private static final String EMAIL = "user@example.com";
  private static final String EMAIL_KEY = "signup:v1:" + EMAIL;
  private static final String IP = "192.0.2.10";
  private static final String IP_KEY = "signup:v1:" + IP;
  private StringRedisTemplate redis;
  private ValueOperations<String, String> values;
  private SignUpRateLimitServiceImpl service;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redis = mock(StringRedisTemplate.class);
    values = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(values);
    service = new SignUpRateLimitServiceImpl(redis);
    ReflectionTestUtils.setField(service, "signUpEmailRateLimit", 3);
    ReflectionTestUtils.setField(service, "signUpEmailRateLimitHour", 1);
    ReflectionTestUtils.setField(service, "signUpIpRateLimit", 5);
    ReflectionTestUtils.setField(service, "signUpIpRateLimitMinutes", 10);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "2"})
  void emailBelowLimitIsRecordedWithAnHourlyWindow(String currentCount) {
    when(values.get(EMAIL_KEY)).thenReturn(currentCount);
    when(values.increment(EMAIL_KEY)).thenReturn(1L);

    service.insertOrUpdateEmailRateLimit(signUp(EMAIL));

    verify(values).increment(EMAIL_KEY);
    verify(redis).expire(EMAIL_KEY, Duration.ofHours(1));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "4"})
  void ipBelowLimitIsRecordedWithAMinuteWindow(String currentCount) {
    when(values.get(IP_KEY)).thenReturn(currentCount);
    when(values.increment(IP_KEY)).thenReturn(1L);

    service.insertOrUpdateIpRateLimit(request(IP));

    verify(values).increment(IP_KEY);
    verify(redis).expire(IP_KEY, Duration.ofMinutes(10));
  }

  @ParameterizedTest
  @ValueSource(strings = {"3", "4"})
  void emailAtOrAboveLimitIsRejectedWithoutChangingCounter(String currentCount) {
    when(values.get(EMAIL_KEY)).thenReturn(currentCount);

    assertThrows(
        ExceedRateLimitException.class, () -> service.insertOrUpdateEmailRateLimit(signUp(EMAIL)));

    verify(values, never()).increment(EMAIL_KEY);
    verify(redis, never()).expire(any(String.class), any(Duration.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"5", "6"})
  void ipAtOrAboveLimitIsRejectedWithoutChangingCounter(String currentCount) {
    when(values.get(IP_KEY)).thenReturn(currentCount);

    assertThrows(
        ExceedRateLimitException.class, () -> service.insertOrUpdateIpRateLimit(request(IP)));

    verify(values, never()).increment(IP_KEY);
    verify(redis, never()).expire(any(String.class), any(Duration.class));
  }

  @Test
  void nullRedisEmailIncrementDoesNotSetExpiry() {
    when(values.increment(EMAIL_KEY)).thenReturn(null);
    service.insertOrUpdateEmailRateLimit(signUp(EMAIL));
    verify(redis, never()).expire(any(String.class), any(Duration.class));
  }

  @Test
  void nullRedisIpIncrementDoesNotSetExpiry() {
    when(values.increment(IP_KEY)).thenReturn(null);
    service.insertOrUpdateIpRateLimit(request(IP));
    verify(redis, never()).expire(any(String.class), any(Duration.class));
  }

  @Test
  void nullRequestsDoNotAccessRedis() {
    service.insertOrUpdateEmailRateLimit(null);
    service.insertOrUpdateIpRateLimit(null);
    verifyNoInteractions(redis);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void missingEmailDoesNotAccessRedis(String email) {
    service.insertOrUpdateEmailRateLimit(signUp(email));
    verifyNoInteractions(redis);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void missingIpDoesNotAccessRedis(String ip) {
    service.insertOrUpdateIpRateLimit(request(ip));
    verifyNoInteractions(redis);
  }

  private SignUpRequest signUp(String email) {
    SignUpRequest request = new SignUpRequest();
    request.setEmail(email);
    return request;
  }

  private HttpServletRequest request(String ip) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn(ip);
    return request;
  }
}
