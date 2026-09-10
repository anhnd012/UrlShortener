package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(
    name = "app.auth.password-reset.email-mode", havingValue = "log", matchIfMissing = true)
public class LoggingPasswordResetEmailSender implements PasswordResetEmailSender {

  @Override
  public void send(String recipientEmail, String resetLink) {
    log.warn(
        "DEV ONLY password reset link for {}: {}. Replace the logging sender before production.",
        recipientEmail,
        resetLink);
  }
}
