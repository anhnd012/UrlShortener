package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordException;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordResetTokenException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.ForgotPasswordRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.ResetPasswordRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetEmailSender;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetRateLimitService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetTokenService;
import com.appsdeveloperblog.ws.urlshorten.auth.validation.PasswordPolicy;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

  private static final String RESET_PATH = "/reset-password#token=";

  private final UserRepository userRepository;
  private final PasswordResetTokenService passwordResetTokenService;
  private final PasswordResetEmailSender passwordResetEmailSender;
  private final PasswordResetRateLimitService passwordResetRateLimitService;
  private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
  private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

  @Value("${app.frontend-base-url:http://localhost:5173}")
  private String frontendBaseUrl;

  @Override
  public void requestReset(ForgotPasswordRequest request, HttpServletRequest httpServletRequest) {
    String normalizedEmail = normalizeEmail(request.getEmail());
    passwordResetRateLimitService.checkAndRecord(httpServletRequest, normalizedEmail);

    userRepository
        .findByEmailIgnoreCase(normalizedEmail)
        .ifPresent(
            user -> {
              String rawToken = passwordResetTokenService.createToken(user);
              passwordResetEmailSender.send(user.getEmail(), buildResetLink(rawToken));
            });
  }

  @Override
  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    String newPassword = request.getNewPassword();
    if (!Objects.equals(newPassword, request.getConfirmPassword())) {
      throw new InvalidPasswordException(List.of("Passwords do not match."));
    }

    PasswordPolicy.validate(newPassword);
    UUID userId = passwordResetTokenService.consumeToken(request.getToken());

    User user =
        userRepository.findById(userId).orElseThrow(InvalidPasswordResetTokenException::new);
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.saveAndFlush(user);

    invalidateAllSessions(user.getEmail());
  }

  private void invalidateAllSessions(String email) {
    Set<String> sessionIds = new HashSet<>();
    if (email != null) {
      sessionIds.addAll(sessionRepository.findByPrincipalName(email).keySet());
      sessionIds.addAll(
          sessionRepository.findByPrincipalName(email.toLowerCase(Locale.ROOT)).keySet());
    }

    sessionIds.forEach(sessionRepository::deleteById);
  }

  private String buildResetLink(String rawToken) {
    String baseUrl =
        frontendBaseUrl.endsWith("/")
            ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
            : frontendBaseUrl;
    return baseUrl + RESET_PATH + rawToken;
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
