package com.appsdeveloperblog.ws.urlshorten.url.service.impl;

import com.appsdeveloperblog.ws.urlshorten.url.service.RedirectCacheService;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectCacheServiceImpl implements RedirectCacheService {
  private static final String REDIRECT_CACHE_KEY_PREFIX = "redirect";
  private static final String REDIRECT_CACHE_KEY_VERSION = "v1";

  private final StringRedisTemplate redisTemplate;

  @Value("${app.cache.redirect-ttl}")
  private Duration redirectCacheTTL;

  @Override
  public void put(String shortCode, String longUrl, long expiresAtMillis) {
    // Logic to store the mapping of shortCode to longUrl in cache
    if (!StringUtils.hasText(shortCode) || !StringUtils.hasText(longUrl)) {
      return;
    }
    if (redirectCacheTTL == null || redirectCacheTTL.isZero() || redirectCacheTTL.isNegative()) {
      return;
    }
    if (expiresAtMillis < 0) {
      return;
    }
    Instant now = Instant.now();
    Instant expiredAt = Instant.ofEpochMilli(expiresAtMillis);
    Duration remainingDuration = Duration.between(now, expiredAt);
    if (remainingDuration.isNegative() || remainingDuration.isZero()) {
      return;
    }
    Duration ttl =
        remainingDuration.compareTo(redirectCacheTTL) < 0 ? remainingDuration : redirectCacheTTL;
    try {
      redisTemplate.opsForValue().set(buildKey(shortCode), longUrl, ttl);
    } catch (RuntimeException e) {
      // Handle exception
      log.warn("Failed to put shortCode {} into cache: {}", shortCode, e.getMessage());
    }
  }

  @Override
  public Optional<String> getLongUrl(String shortCode) {
    // Logic to retrieve the longUrl for the given shortCode from cache
    if (!StringUtils.hasText(shortCode)) {
      return Optional.empty();
    }
    String longUrl;
    try {
      longUrl = redisTemplate.opsForValue().get(buildKey(shortCode));
    } catch (RuntimeException e) {
      log.warn("Failed to get shortCode {} from cache: {}", shortCode, e.getMessage());
      return Optional.empty();
    }
    return Optional.ofNullable(longUrl);
  }

  private String buildKey(String shortCode) {
    return REDIRECT_CACHE_KEY_PREFIX + ":" + REDIRECT_CACHE_KEY_VERSION + ":" + shortCode;
  }
}
