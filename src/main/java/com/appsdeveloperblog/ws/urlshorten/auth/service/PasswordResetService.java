package com.appsdeveloperblog.ws.urlshorten.auth.service;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.ForgotPasswordRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.ResetPasswordRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface PasswordResetService {

  void requestReset(ForgotPasswordRequest request, HttpServletRequest httpServletRequest);

  void resetPassword(ResetPasswordRequest request);
}
