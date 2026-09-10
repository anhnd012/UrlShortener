package com.appsdeveloperblog.ws.urlshorten.common.lock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

class DistributedLockServiceTest {

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private DistributedLockService lockService;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    lockService = new DistributedLockService(redisTemplate);
  }

  @Test
  void acquireLockReturnsTrueWhenSetIfAbsentSucceeds() {
    when(valueOperations.setIfAbsent("lock:test", "val-123", Duration.ofMinutes(5)))
        .thenReturn(true);

    boolean result = lockService.acquireLock("lock:test", "val-123", Duration.ofMinutes(5));

    assertTrue(result);
  }

  @Test
  void acquireLockReturnsFalseWhenAlreadyLocked() {
    when(valueOperations.setIfAbsent("lock:test", "val-123", Duration.ofMinutes(5)))
        .thenReturn(false);

    boolean result = lockService.acquireLock("lock:test", "val-123", Duration.ofMinutes(5));

    assertFalse(result);
  }

  @Test
  void acquireLockHandlesRedisExceptionGracefully() {
    when(valueOperations.setIfAbsent("lock:test", "val-123", Duration.ofMinutes(5)))
        .thenThrow(new RuntimeException("Redis connection refused"));

    boolean result = lockService.acquireLock("lock:test", "val-123", Duration.ofMinutes(5));

    assertFalse(result);
  }

  @Test
  void releaseLockExecutesLuaScript() {
    lockService.releaseLock("lock:test", "val-123");

    verify(redisTemplate).execute(
        any(RedisScript.class),
        eq(Collections.singletonList("lock:test")),
        eq("val-123"));
  }
}
