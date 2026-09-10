package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.ForgotPasswordRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.ResetPasswordRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetEmailSender;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetRateLimitService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetTokenService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.util.ReflectionTestUtils;

class PasswordResetServiceImplTest {

  private UserRepository userRepository;
  private PasswordResetTokenService tokenService;
  private PasswordResetEmailSender emailSender;
  private PasswordResetRateLimitService rateLimitService;
  private PasswordEncoder passwordEncoder;
  private FindByIndexNameSessionRepository<Session> sessionRepository;
  private PasswordResetServiceImpl passwordResetService;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    tokenService = mock(PasswordResetTokenService.class);
    emailSender = mock(PasswordResetEmailSender.class);
    rateLimitService = mock(PasswordResetRateLimitService.class);
    passwordEncoder = mock(PasswordEncoder.class);
    sessionRepository = mock(FindByIndexNameSessionRepository.class);
    passwordResetService =
        new PasswordResetServiceImpl(
            userRepository,
            tokenService,
            emailSender,
            rateLimitService,
            passwordEncoder,
            sessionRepository);
    ReflectionTestUtils.setField(passwordResetService, "frontendBaseUrl", "http://localhost:5173");
  }

  @Test
  void requestsResetWithGenericBehaviorForExistingUser() {
    UUID userId = UUID.randomUUID();
    User user = user(userId, "user@example.com");
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail(" User@Example.COM ");
    HttpServletRequest httpServletRequest = new MockHttpServletRequest();
    when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
    when(tokenService.createToken(user)).thenReturn("raw-token");

    passwordResetService.requestReset(request, httpServletRequest);

    verify(rateLimitService).checkAndRecord(httpServletRequest, "user@example.com");
    verify(emailSender).send("user@example.com", "http://localhost:5173/reset-password#token=raw-token");
  }

  @Test
  void doesNotRevealOrSendAnythingForUnknownUser() {
    ForgotPasswordRequest request = new ForgotPasswordRequest();
    request.setEmail("unknown@example.com");
    when(userRepository.findByEmailIgnoreCase("unknown@example.com"))
        .thenReturn(Optional.empty());

    passwordResetService.requestReset(request, new MockHttpServletRequest());

    verify(rateLimitService).checkAndRecord(any(), eq("unknown@example.com"));
    verify(emailSender, never()).send(any(), any());
    verifyNoInteractions(tokenService);
  }

  @Test
  void resetsPasswordAndInvalidatesIndexedSessions() {
    UUID userId = UUID.randomUUID();
    User user = user(userId, "user@example.com");
    ResetPasswordRequest request = resetRequest("new-password-123");
    Session session = mock(Session.class);
    when(tokenService.consumeToken("raw-token")).thenReturn(userId);
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(passwordEncoder.encode("new-password-123")).thenReturn("encoded-password");
    when(sessionRepository.findByPrincipalName(any())).thenReturn(Map.of("session-1", session));

    passwordResetService.resetPassword(request);

    assertEquals("encoded-password", user.getPasswordHash());
    verify(userRepository).saveAndFlush(user);
    verify(sessionRepository).deleteById("session-1");
  }

  @Test
  void rejectsMismatchedPasswordsBeforeConsumingToken() {
    ResetPasswordRequest request = resetRequest("new-password-123");
    request.setConfirmPassword("different-password-123");

    assertThrows(InvalidPasswordException.class, () -> passwordResetService.resetPassword(request));

    verifyNoInteractions(tokenService, userRepository, passwordEncoder, sessionRepository);
  }

  private User user(UUID id, String email) {
    User user = new User();
    ReflectionTestUtils.setField(user, "id", id);
    user.setEmail(email);
    return user;
  }

  private ResetPasswordRequest resetRequest(String password) {
    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("raw-token");
    request.setNewPassword(password);
    request.setConfirmPassword(password);
    return request;
  }
}
