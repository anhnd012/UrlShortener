package com.appsdeveloperblog.ws.urlshorten.auth.service;

public interface PasswordResetEmailSender {

  void send(String recipientEmail, String resetLink);
}
