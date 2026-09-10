package com.appsdeveloperblog.ws.urlshorten.common.lock;

import java.time.Duration;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

  private static final RedisScript<Long> UNLOCK_SCRIPT =
      new DefaultRedisScript<>(
          "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
          Long.class);

  private final StringRedisTemplate redisTemplate;

  /**
   * Attempts to acquire a distributed lock using atomic SET NX EX.
   *
   * @param lockKey the unique redis lock key
   * @param lockValue unique value identifying the instance/caller
   * @param ttl maximum duration before the lock expires automatically
   * @return true if lock was successfully acquired, false if held by another instance
   */
  public boolean acquireLock(String lockKey, String lockValue, Duration ttl) {
    try {
      Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl);
      return Boolean.TRUE.equals(result);
    } catch (Exception e) {
      log.warn(
          "Failed to acquire distributed lock for key '{}': {}. Assuming lock not acquired.",
          lockKey,
          e.getMessage());
      return false;
    }
  }

  /**
   * Safely releases the distributed lock only if the current value matches the lockValue.
   *
   * @param lockKey the unique redis lock key
   * @param lockValue the lock value passed when acquired
   */
  public void releaseLock(String lockKey, String lockValue) {
    try {
      redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
      log.debug("Released distributed lock for key '{}'", lockKey);
    } catch (Exception e) {
      log.warn("Failed to release distributed lock for key '{}': {}", lockKey, e.getMessage());
    }
  }
}
