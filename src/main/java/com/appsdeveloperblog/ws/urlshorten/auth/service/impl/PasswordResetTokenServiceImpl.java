package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordResetTokenException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

  private static final String KEY_PREFIX = "password-reset:v1:token:";
  private static final int TOKEN_BYTES = 32;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final StringRedisTemplate redisTemplate;

  @Value("${app.auth.password-reset.token-ttl:15m}")
  private Duration tokenTtl;

  @Override
  public String createToken(User user) {
    if (user == null || user.getId() == null) {
      throw new IllegalArgumentException("A persisted user is required to create a reset token.");
    }

    byte[] randomBytes = new byte[TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(randomBytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

    redisTemplate
        .opsForValue()
        .set(buildKey(rawToken), user.getId().toString(), tokenTtl);

    return rawToken;
  }

  @Override
  public UUID consumeToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new InvalidPasswordResetTokenException();
    }

    String userId = redisTemplate.opsForValue().getAndDelete(buildKey(rawToken));
    if (userId == null || userId.isBlank()) {
      throw new InvalidPasswordResetTokenException();
    }

    try {
      return UUID.fromString(userId);
    } catch (IllegalArgumentException exception) {
      throw new InvalidPasswordResetTokenException();
    }
  }

  public String buildKey(String rawToken) {
    return KEY_PREFIX + sha256(rawToken);
  }

  private String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available.", exception);
    }
  }
}
