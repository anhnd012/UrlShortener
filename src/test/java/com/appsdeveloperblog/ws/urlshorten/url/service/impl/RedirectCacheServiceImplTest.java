package com.appsdeveloperblog.ws.urlshorten.url.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class RedirectCacheServiceImplTest {

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private RedirectCacheServiceImpl cacheService;

  @BeforeEach
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    cacheService = new RedirectCacheServiceImpl(redisTemplate);
    ReflectionTestUtils.setField(cacheService, "redirectCacheTTL", Duration.ofMinutes(10));
  }

  @Test
  void putWhenValuesAreValidUsesConfiguredTtlAndVersionedKey() {
    long expiresAtMillis = Instant.now().plus(Duration.ofHours(1)).toEpochMilli();

    cacheService.put("aB12xYz9", "https://example.com/article", expiresAtMillis);

    verify(valueOperations)
        .set("redirect:v1:aB12xYz9", "https://example.com/article", Duration.ofMinutes(10));
  }

  @Test
  void putWhenUrlExpiresBeforeConfiguredTtlUsesRemainingLifetime() {
    long expiresAtMillis = Instant.now().plus(Duration.ofSeconds(30)).toEpochMilli();
    ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

    cacheService.put("aB12xYz9", "https://example.com/article", expiresAtMillis);

    verify(valueOperations)
        .set(eq("redirect:v1:aB12xYz9"), eq("https://example.com/article"), ttlCaptor.capture());
    Duration actualTtl = ttlCaptor.getValue();
    assertTrue(actualTtl.isPositive());
    assertTrue(actualTtl.compareTo(Duration.ofSeconds(30)) <= 0);
  }

  @Test
  void putWhenShortCodeOrLongUrlIsBlankDoesNotWriteToRedis() {
    long expiresAtMillis = Instant.now().plus(Duration.ofHours(1)).toEpochMilli();

    cacheService.put(" ", "https://example.com/article", expiresAtMillis);
    cacheService.put("aB12xYz9", "", expiresAtMillis);

    verify(valueOperations, never()).set(any(), any(), any(Duration.class));
  }

  @Test
  void putWhenConfiguredTtlIsNotUsableDoesNotWriteToRedis() {
    long expiresAtMillis = Instant.now().plus(Duration.ofHours(1)).toEpochMilli();

    ReflectionTestUtils.setField(cacheService, "redirectCacheTTL", null);
    cacheService.put("aB12xYz9", "https://example.com/article", expiresAtMillis);
    ReflectionTestUtils.setField(cacheService, "redirectCacheTTL", Duration.ZERO);
    cacheService.put("aB12xYz9", "https://example.com/article", expiresAtMillis);
    ReflectionTestUtils.setField(cacheService, "redirectCacheTTL", Duration.ofSeconds(-1));
    cacheService.put("aB12xYz9", "https://example.com/article", expiresAtMillis);

    verify(valueOperations, never()).set(any(), any(), any(Duration.class));
  }

  @Test
  void putWhenExpirationIsInvalidDoesNotWriteToRedis() {
    cacheService.put("aB12xYz9", "https://example.com/article", -1);
    cacheService.put(
        "aB12xYz9",
        "https://example.com/article",
        Instant.now().minus(Duration.ofSeconds(1)).toEpochMilli());

    verify(valueOperations, never()).set(any(), any(), any(Duration.class));
  }

  @Test
  void putWhenRedisFailsDoesNotPropagateInfrastructureFailure() {
    doThrow(new IllegalStateException("Redis unavailable"))
        .when(valueOperations)
        .set(eq("redirect:v1:aB12xYz9"), eq("https://example.com/article"), any(Duration.class));

    cacheService.put(
        "aB12xYz9",
        "https://example.com/article",
        Instant.now().plus(Duration.ofHours(1)).toEpochMilli());

    verify(valueOperations)
        .set(eq("redirect:v1:aB12xYz9"), eq("https://example.com/article"), any(Duration.class));
  }

  @Test
  void getLongUrlWhenCacheContainsValueReturnsIt() {
    when(valueOperations.get("redirect:v1:aB12xYz9")).thenReturn("https://example.com/article");

    Optional<String> result = cacheService.getLongUrl("aB12xYz9");

    assertEquals(Optional.of("https://example.com/article"), result);
  }

  @Test
  void getLongUrlWhenValueIsMissingOrRedisFailsReturnsEmpty() {
    assertTrue(cacheService.getLongUrl("missing1").isEmpty());

    when(valueOperations.get("redirect:v1:broken12"))
        .thenThrow(new IllegalStateException("Redis unavailable"));
    assertTrue(cacheService.getLongUrl("broken12").isEmpty());
  }

  @Test
  void getLongUrlWhenShortCodeIsBlankDoesNotReadRedis() {
    assertTrue(cacheService.getLongUrl(" ").isEmpty());

    verify(valueOperations, never()).get(any());
  }
}
