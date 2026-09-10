package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.ExceedRateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class SignInRateLimitServiceImplTest {

  private static final String IP_ADDRESS = "127.0.0.1";
  private static final String REDIS_KEY = "signin:v1:" + IP_ADDRESS;

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private HttpServletRequest request;
  private SignInRateLimitServiceImpl rateLimitService;

  @BeforeEach
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    request = mock(HttpServletRequest.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(request.getRemoteAddr()).thenReturn(IP_ADDRESS);

    rateLimitService = new SignInRateLimitServiceImpl(redisTemplate);
    ReflectionTestUtils.setField(rateLimitService, "signInIpRateLimit", 5);
    ReflectionTestUtils.setField(rateLimitService, "signInIpRateLimitMinutes", 30);
  }

  @Test
  void checkOnlyReadsCounterWhenLimitHasNotBeenReached() {
    when(valueOperations.get(REDIS_KEY)).thenReturn("4");

    assertDoesNotThrow(() -> rateLimitService.checkIpRateLimit(request));

    verify(valueOperations, never()).increment(REDIS_KEY);
  }

  @Test
  void checkRejectsRequestWhenLimitHasBeenReached() {
    when(valueOperations.get(REDIS_KEY)).thenReturn("5");

    assertThrows(
        ExceedRateLimitException.class, () -> rateLimitService.checkIpRateLimit(request));

    verify(valueOperations, never()).increment(REDIS_KEY);
  }

  @Test
  void failedAuthenticationIncrementsCounterAndCreatesTtlForFirstFailure() {
    when(valueOperations.increment(REDIS_KEY)).thenReturn(1L);

    rateLimitService.recordFailedAuthentication(request);

    verify(valueOperations).increment(REDIS_KEY);
    verify(redisTemplate).expire(REDIS_KEY, Duration.ofMinutes(30));
  }

  @Test
  void failedAuthenticationDoesNotResetTtlForExistingCounter() {
    when(valueOperations.increment(REDIS_KEY)).thenReturn(2L);

    rateLimitService.recordFailedAuthentication(request);

    verify(valueOperations).increment(REDIS_KEY);
    verify(redisTemplate, never()).expire(any(String.class), any(Duration.class));
  }
}
