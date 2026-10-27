package com.appsdeveloperblog.ws.urlshorten.config;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordConfigTest {
  private UserDetailsService userDetailsService;

  @BeforeEach
  void setUp() {
    userDetailsService = mock(UserDetailsService.class);
  }

//  @Test
//  void argon2PasswordEncoderHashesAndMatchesPasswords() {
//    PasswordEncoder encoder = new SecurityConfig(userDetailsService).passwordEncoder();
//
//    String rawPassword = "correct horse battery staple";
//    String encodedPassword = encoder.encode(rawPassword);
//
//    assertNotEquals(rawPassword, encodedPassword);
//    assertTrue(encoder.matches(rawPassword, encodedPassword));
//  }
}
