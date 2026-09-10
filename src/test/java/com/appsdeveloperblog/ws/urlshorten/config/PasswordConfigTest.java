package com.appsdeveloperblog.ws.urlshorten.config;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordConfigTest {

  @Test
  void argon2PasswordEncoderHashesAndMatchesPasswords() {
    PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

    String rawPassword = "correct horse battery staple";
    String encodedPassword = encoder.encode(rawPassword);

    assertNotEquals(rawPassword, encodedPassword);
    assertTrue(encoder.matches(rawPassword, encodedPassword));
  }
}
