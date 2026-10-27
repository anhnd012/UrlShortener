package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {
    private final StringRedisTemplate stringRedisTemplate;

    private final String REFRESH_TOKEN_KEY = "refresh_token:v1";

    @Override
    public void saveRefreshToken(String refreshToken, long expInSeconds) {
        stringRedisTemplate.opsForValue().set(REFRESH_TOKEN_KEY + refreshToken, refreshToken, Duration.ofDays(7));
    }
}
