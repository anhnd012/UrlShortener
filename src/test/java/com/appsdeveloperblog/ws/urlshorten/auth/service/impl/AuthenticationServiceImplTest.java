package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.AuthenticatedUser;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignInRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.AccessTokenService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetTokenService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.RedisService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.SignInRateLimitService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.SignUpRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

class AuthenticationServiceImplTest {
  private UserRepository userRepository;
  private AuthenticationServiceImpl authenticationService;
  private PasswordEncoder passwordEncoder;
  private SignUpRateLimitService signupRateLimitService;
  private SignInRateLimitService signInRateLimitService;
  private AuthenticationManager authenticationManager;
  private SecurityContextRepository securityContextRepository;
  private RedisService redisService;
  private PasswordResetTokenService passwordResetTokenService;
  private AccessTokenService accessTokenService;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    signupRateLimitService = mock(SignUpRateLimitService.class);
    signInRateLimitService = mock(SignInRateLimitService.class);
    authenticationManager = mock(AuthenticationManager.class);
    securityContextRepository = mock(SecurityContextRepository.class);
    redisService = mock(RedisService.class);
    passwordResetTokenService = mock(PasswordResetTokenService.class);
    accessTokenService = mock(AccessTokenService.class);
    authenticationService =
        new AuthenticationServiceImpl(
            passwordEncoder,
            userRepository,
            signupRateLimitService,
            signInRateLimitService,
            authenticationManager,
            securityContextRepository,
            accessTokenService,
            passwordResetTokenService,
            redisService);
    ReflectionTestUtils.setField(authenticationService, "refreshTokenExpiration", 900L);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void acceptsLongPassphraseWithoutCompositionRules() {
    assertDoesNotThrow(
        () -> authenticationService.validatePassword("calm river walks under a silver moon"));
  }

  @Test
  void rejectsPasswordOutsideTheLengthLimits() {
    InvalidPasswordException tooShort =
        assertThrows(
            InvalidPasswordException.class, () -> authenticationService.validatePassword("short"));

    assertTrue(tooShort.getViolations().stream().anyMatch(message -> message.contains("12")));

    assertThrows(
        InvalidPasswordException.class,
        () -> authenticationService.validatePassword("a".repeat(129)));
  }

  @Test
  void rejectsKnownCompromisedAndRepeatedPasswords() {
    assertThrows(
        InvalidPasswordException.class,
        () -> authenticationService.validatePassword("password123"));

    assertThrows(
        InvalidPasswordException.class,
        () -> authenticationService.validatePassword("a".repeat(20)));
  }

  @Test
  void successfulSignInNormalizesEmailAndReturnsTokensWithoutSavingASession() {
    SignInRequest request = new SignInRequest();
    request.setEmail(" User@Example.com ");
    request.setPassword("correct-password");

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
    Authentication authenticated = mock(Authentication.class);
    UUID userId = UUID.randomUUID();
    when(authenticated.getPrincipal())
        .thenReturn(new AuthenticatedUser(userId, "user@example.com", "hash", List.of()));
    when(authenticationManager.authenticate(any())).thenReturn(authenticated);
    when(accessTokenService.create(userId)).thenReturn("access-token");
    when(passwordResetTokenService.createRandomRefreshToken()).thenReturn("refresh-token");

    var response = authenticationService.signIn(request, httpServletRequest, httpServletResponse);

    verify(authenticationManager)
        .authenticate(
            argThat(
                authenticationRequest ->
                    "user@example.com".equals(authenticationRequest.getPrincipal())
                        && "correct-password".equals(authenticationRequest.getCredentials())
                        && !authenticationRequest.isAuthenticated()));
    assertEquals("access-token", response.getAccessToken());
    assertEquals("refresh-token", response.getRefreshToken());
    verify(accessTokenService).create(userId);
    verify(redisService).saveRefreshToken("refresh-token", 900L);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(securityContextRepository);
  }

  @Test
  void invalidCredentialsReturnGenericErrorWithoutIssuingOrSavingTokens() {
    SignInRequest request = new SignInRequest();
    request.setEmail("user@example.com");
    request.setPassword("wrong-password");

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
    when(authenticationManager.authenticate(any()))
        .thenThrow(new BadCredentialsException("invalid credentials"));

    assertThrows(
        com.appsdeveloperblog.ws.urlshorten.auth.exception.AuthenticationException.class,
        () -> authenticationService.signIn(request, httpServletRequest, httpServletResponse));

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(
        accessTokenService, passwordResetTokenService, redisService, securityContextRepository);
  }

  @ParameterizedTest
  @MethodSource("invalidSignInRequests")
  void rejectsInvalidSignInRequestBeforeAuthentication(SignInRequest request) {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            authenticationService.signIn(
                request, mock(HttpServletRequest.class), mock(HttpServletResponse.class)));

    verifyNoInteractions(
        authenticationManager,
        securityContextRepository,
        signInRateLimitService,
        accessTokenService,
        passwordResetTokenService,
        redisService);
  }

  private static Stream<Arguments> invalidSignInRequests() {
    return Stream.of(
        Arguments.of((SignInRequest) null),
        Arguments.of(signInRequest(null, "password")),
        Arguments.of(signInRequest(" ", "password")),
        Arguments.of(signInRequest("user@example.com", null)),
        Arguments.of(signInRequest("user@example.com", "")),
        Arguments.of(signInRequest("user@example.com", " ")));
  }

  private static SignInRequest signInRequest(String email, String password) {
    SignInRequest request = new SignInRequest();
    request.setEmail(email);
    request.setPassword(password);
    return request;
  }
}
