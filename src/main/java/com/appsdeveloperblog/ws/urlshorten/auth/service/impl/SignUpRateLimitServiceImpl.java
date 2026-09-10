package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.ExceedRateLimitException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignUpRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.service.SignUpRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SignUpRateLimitServiceImpl implements SignUpRateLimitService {
    private final StringRedisTemplate redisTemplate;
    private final String REDIS_KEY_PREFIX = "signup";
    private final String REDIS_KEY_VERSION = "v1";

    @Value("${app.redis.sign-up-email-rate-limit-hour}")
    private Integer signUpEmailRateLimitHour;

    @Value("${app.redis.sign-up-email-rate-limit}")
    private Integer signUpEmailRateLimit;

    @Value("${app.redis.sign-up-ip-rate-limit}")
    private Integer signUpIpRateLimit;

    @Value("${app.redis.sign-up-ip-rate-limit-minutes}")
    private Integer signUpIpRateLimitMinutes;


    private String buildRateLimitKey(String key) {
        return String.format("%s:%s:%s", REDIS_KEY_PREFIX, REDIS_KEY_VERSION, key);
    }

    @Override
    public void insertOrUpdateEmailRateLimit(SignUpRequest request) {
        if(request == null || StringUtils.isBlank(request.getEmail())) {
            return;
        }

        String key = buildRateLimitKey(request.getEmail());
        String currentValue = redisTemplate.opsForValue().get(key);
        Long currentCount = (currentValue == null || currentValue.isBlank()) ? 0 : Long.valueOf(currentValue);
        if(currentCount >= signUpEmailRateLimit) {
            throw new ExceedRateLimitException("Email rate limit exceeded");
        }
        Long count = redisTemplate.opsForValue().increment(key);

        if(count != null) {
            redisTemplate.expire(key, Duration.ofHours(signUpEmailRateLimitHour));
        }
    }

    @Override
    public void insertOrUpdateIpRateLimit(HttpServletRequest httpServletRequest) {
        if(httpServletRequest == null || StringUtils.isBlank(httpServletRequest.getRemoteAddr())) {
            return;
        }

        String ipAddress = httpServletRequest.getRemoteAddr();
        String key = buildRateLimitKey(ipAddress);

        String currentValue = redisTemplate.opsForValue().get(key);
        Long currentCount = (currentValue == null || currentValue.isBlank()) ? 0 : Long.valueOf(currentValue);
        if(currentCount >= signUpIpRateLimit) {
            throw new ExceedRateLimitException("Ip rate limit exceeded");
        }
        Long count = redisTemplate.opsForValue().increment(key);
        if(count != null) {
            redisTemplate.expire(key, Duration.ofMinutes(signUpIpRateLimitMinutes));
        }

    }
}
