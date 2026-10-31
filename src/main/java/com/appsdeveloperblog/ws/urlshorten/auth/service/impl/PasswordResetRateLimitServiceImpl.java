package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.ExceedRateLimitException;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PasswordResetRateLimitServiceImpl implements PasswordResetRateLimitService {

  private static final String KEY_PREFIX = "password-reset:v1:rate-limit:";
  private static final String TOO_MANY_REQUESTS_MESSAGE =
      "Too many password reset requests. Please try again later.";

  private final StringRedisTemplate redisTemplate;

  @Value("${app.redis.password-reset-ip-rate-limit:10}")
  private Integer ipLimit;

  @Value("${app.redis.password-reset-ip-rate-limit-minutes:60}")
  private Integer ipWindowMinutes;

  @Value("${app.redis.password-reset-email-rate-limit:3}")
  private Integer emailLimit;

  @Value("${app.redis.password-reset-email-rate-limit-hours:1}")
  private Integer emailWindowHours;

  @Override
  public void checkAndRecord(HttpServletRequest httpServletRequest, String normalizedEmail) {
    String ipAddress = httpServletRequest == null ? null : httpServletRequest.getRemoteAddr();
    String emailKey = buildKey("email:" + sha256(normalizedEmail));
    String ipKey = StringUtils.hasText(ipAddress) ? buildKey("ip:" + ipAddress) : null;

    checkLimit(emailKey, emailLimit);
    if (ipKey != null) {
      checkLimit(ipKey, ipLimit);
    }

    increment(emailKey, Duration.ofHours(emailWindowHours));
    if (ipKey != null) {
      increment(ipKey, Duration.ofMinutes(ipWindowMinutes));
    }
  }

  private void checkLimit(String key, int limit) {
    String currentValue = redisTemplate.opsForValue().get(key);
    long currentCount =
        currentValue == null || currentValue.isBlank() ? 0 : Long.parseLong(currentValue);
    if (currentCount >= limit) {
      throw new ExceedRateLimitException(TOO_MANY_REQUESTS_MESSAGE);
    }
  }

  private void increment(String key, Duration window) {
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redisTemplate.expire(key, window);
    }
  }

  private String buildKey(String suffix) {
    return KEY_PREFIX + suffix;
  }

  private String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }
}
