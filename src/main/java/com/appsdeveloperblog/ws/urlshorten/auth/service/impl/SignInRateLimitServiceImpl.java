package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.ExceedRateLimitException;
import com.appsdeveloperblog.ws.urlshorten.auth.service.SignInRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignInRateLimitServiceImpl implements SignInRateLimitService {
  private final StringRedisTemplate redisTemplate;
  private final String REDIS_KEY_PREFIX = "signin";
  private final String REDIS_KEY_VERSION = "v1";

  @Value("${app.redis.sign-in-ip-rate-limit}")
  private Integer signInIpRateLimit;

  @Value("${app.redis.sign-in-ip-rate-limit-minutes}")
  private Integer signInIpRateLimitMinutes;

  @Override
  public void checkIpRateLimit(HttpServletRequest httpServletRequest) {
    if (httpServletRequest == null || StringUtils.isBlank(httpServletRequest.getRemoteAddr())) {
      return;
    }

    String ipAddress = httpServletRequest.getRemoteAddr();
    String key = buildRateLimitKey(ipAddress);

    long currentCount = getCurrentCount(key);
    if (currentCount >= signInIpRateLimit) {
      throw new ExceedRateLimitException("Ip rate limit exceeded");
    }
  }

  @Override
  public void recordFailedAuthentication(HttpServletRequest httpServletRequest) {
    if (httpServletRequest == null || StringUtils.isBlank(httpServletRequest.getRemoteAddr())) {
      return;
    }

    String ipAddress = httpServletRequest.getRemoteAddr();
    String key = buildRateLimitKey(ipAddress);
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redisTemplate.expire(key, Duration.ofMinutes(signInIpRateLimitMinutes));
    }
  }

  private long getCurrentCount(String key) {
    String currentValue = redisTemplate.opsForValue().get(key);
    return (currentValue == null || currentValue.isBlank()) ? 0 : Long.parseLong(currentValue);
  }

  public String buildRateLimitKey(String key) {
    return String.format("%s:%s:%s", REDIS_KEY_PREFIX, REDIS_KEY_VERSION, key);
  }
}
