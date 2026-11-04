package com.appsdeveloperblog.ws.urlshorten.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.AccessTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordConfigTest {

  @Test
  void argon2PasswordEncoderHashesAndMatchesPasswords() {
    SecurityConfig config =
        new SecurityConfig(mock(AccessTokenService.class), mock(UserRepository.class));
    PasswordEncoder encoder = config.passwordEncoder();
    String rawPassword = "correct horse battery staple";

    String encodedPassword = encoder.encode(rawPassword);
    String secondEncodedPassword = encoder.encode(rawPassword);

    assertAll(
        () -> assertNotEquals(rawPassword, encodedPassword),
        () -> assertTrue(encodedPassword.startsWith("$argon2")),
        () -> assertTrue(encoder.matches(rawPassword, encodedPassword)),
        () -> assertFalse(encoder.matches("incorrect password", encodedPassword)),
        () -> assertNotEquals(encodedPassword, secondEncodedPassword));
  }
}
