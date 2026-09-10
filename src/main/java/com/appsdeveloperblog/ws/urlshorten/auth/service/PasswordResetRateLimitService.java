package com.appsdeveloperblog.ws.urlshorten.auth.service;

import jakarta.servlet.http.HttpServletRequest;

public interface PasswordResetRateLimitService {

  void checkAndRecord(HttpServletRequest httpServletRequest, String normalizedEmail);
}
