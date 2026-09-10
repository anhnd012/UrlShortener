package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.ExceedRateLimitException;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignInRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.SignInRateLimitService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.SignUpRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

class AuthenticationServiceImplTest {
  private UserRepository userRepository;
  private AuthenticationServiceImpl authenticationService;
  private PasswordEncoder passwordEncoder;
  private SignUpRateLimitService signupRateLimitService;
  private SignInRateLimitService signInRateLimitService;
  private AuthenticationManager authenticationManager;
  private SecurityContextRepository securityContextRepository;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    signupRateLimitService = mock(SignUpRateLimitService.class);
    signInRateLimitService = mock(SignInRateLimitService.class);
    authenticationManager = mock(AuthenticationManager.class);
    securityContextRepository = mock(SecurityContextRepository.class);
    authenticationService =
        new AuthenticationServiceImpl(
            passwordEncoder,
            userRepository,
            signupRateLimitService,
            signInRateLimitService,
            authenticationManager,
            securityContextRepository);
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
            InvalidPasswordException.class,
            () -> authenticationService.validatePassword("short"));

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
  void authenticatesAndSavesSecurityContextAfterSuccessfulSignIn() {
    SignInRequest request = new SignInRequest();
    request.setEmail("user@example.com");
    request.setPassword("correct-password");

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
    Authentication authenticated = mock(Authentication.class);
    when(authenticationManager.authenticate(any())).thenReturn(authenticated);

    authenticationService.signIn(request, httpServletRequest, httpServletResponse);

    verify(signInRateLimitService).checkIpRateLimit(httpServletRequest);
    verify(authenticationManager)
        .authenticate(
            argThat(
                authenticationRequest ->
                    "user@example.com".equals(authenticationRequest.getPrincipal())
                        && "correct-password".equals(authenticationRequest.getCredentials())
                        && !authenticationRequest.isAuthenticated()));
    assertSame(authenticated, SecurityContextHolder.getContext().getAuthentication());
    verify(signInRateLimitService, never()).recordFailedAuthentication(httpServletRequest);
    verify(securityContextRepository)
        .saveContext(
            argThat(context -> context.getAuthentication() == authenticated),
            same(httpServletRequest),
            same(httpServletResponse));
  }

  @Test
  void recordsFailedAuthenticationAndReturnsGenericError() {
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

    verify(signInRateLimitService).checkIpRateLimit(httpServletRequest);
    verify(signInRateLimitService).recordFailedAuthentication(httpServletRequest);
    verify(securityContextRepository, never())
        .saveContext(any(), same(httpServletRequest), same(httpServletResponse));
  }

  @Test
  void rejectsRequestBeforeAuthenticationWhenIpRateLimitIsExceeded() {
    SignInRequest request = new SignInRequest();
    request.setEmail("user@example.com");
    request.setPassword("correct-password");

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
    doThrow(new ExceedRateLimitException("Ip rate limit exceeded"))
        .when(signInRateLimitService)
        .checkIpRateLimit(httpServletRequest);

    assertThrows(
        ExceedRateLimitException.class,
        () -> authenticationService.signIn(request, httpServletRequest, httpServletResponse));

    verifyNoInteractions(authenticationManager, securityContextRepository);
    verify(signInRateLimitService, never()).recordFailedAuthentication(httpServletRequest);
  }

  @Test
  void rejectsBlankSignInRequestBeforeAuthentication() {
    SignInRequest request = new SignInRequest();
    request.setEmail(" ");
    request.setPassword("password");

    assertThrows(
        IllegalArgumentException.class,
        () ->
            authenticationService.signIn(
                request, mock(HttpServletRequest.class), mock(HttpServletResponse.class)));

    verifyNoInteractions(authenticationManager, securityContextRepository, signInRateLimitService);
  }
}
