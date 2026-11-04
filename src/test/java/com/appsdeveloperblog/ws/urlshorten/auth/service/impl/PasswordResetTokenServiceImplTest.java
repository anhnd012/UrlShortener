package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordResetTokenException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

class PasswordResetTokenServiceImplTest {

  private StringRedisTemplate redisTemplate;
  private ValueOperations<String, String> valueOperations;
  private PasswordResetTokenServiceImpl tokenService;

  @BeforeEach
  void setUp() {
    redisTemplate = mock(StringRedisTemplate.class);
    valueOperations = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    tokenService = new PasswordResetTokenServiceImpl(redisTemplate);
    ReflectionTestUtils.setField(tokenService, "tokenTtl", Duration.ofMinutes(15));
  }

  @Test
  void createsRandomTokenAndStoresOnlyItsHash() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    ReflectionTestUtils.setField(user, "id", userId);

    String rawToken = tokenService.createToken(user);

    assertEquals(43, rawToken.length());
    assertTrue(rawToken.matches("[A-Za-z0-9_-]+"));
    verify(valueOperations)
        .set(
            eq(tokenService.buildKey(rawToken)), eq(userId.toString()), eq(Duration.ofMinutes(15)));
  }

  @Test
  void consumesTokenAtomicallyAndReturnsUserId() {
    UUID userId = UUID.randomUUID();
    when(valueOperations.getAndDelete(anyString())).thenReturn(userId.toString());

    UUID result = tokenService.consumeToken("raw-reset-token");

    assertEquals(userId, result);
    verify(valueOperations).getAndDelete(tokenService.buildKey("raw-reset-token"));
  }

  @Test
  void rejectsMissingOrExpiredToken() {
    when(valueOperations.getAndDelete(anyString())).thenReturn(null);

    assertThrows(
        InvalidPasswordResetTokenException.class,
        () -> tokenService.consumeToken("expired-reset-token"));
  }
}
