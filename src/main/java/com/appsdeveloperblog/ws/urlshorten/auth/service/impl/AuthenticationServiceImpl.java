package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.AuthenticationException;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.EmailAlreadyExistsException;
import com.appsdeveloperblog.ws.urlshorten.auth.validation.PasswordPolicy;
import java.util.Locale;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignInRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignUpRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.AuthenticationService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.SignInRateLimitService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.SignUpRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final SignUpRateLimitService signupRateLimitService;
  private final SignInRateLimitService signInRateLimitService;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextHolderStrategy securityContextHolderStrategy =
          SecurityContextHolder.getContextHolderStrategy();
  private final SecurityContextRepository securityContextRepository;

  @Override
  @Transactional
  public void signUp(SignUpRequest request, HttpServletRequest httpServletRequest) {
     validatePassword(request.getPassword());
     applyRateLimit(request, httpServletRequest);
     User user = new User();
     String hashedPassword = passwordEncoder.encode(request.getPassword());
     user.setEmail(request.getEmail());
     user.setPasswordHash(hashedPassword);
     try {
       userRepository.saveAndFlush(user);
     } catch (DataIntegrityViolationException exception) {
       if (isEmailUniqueViolation(exception)) {
         throw new EmailAlreadyExistsException();
       }

       throw exception;
     }
  }

  

  private void applyRateLimit(SignUpRequest request, HttpServletRequest httpServletRequest) {
    signupRateLimitService.insertOrUpdateEmailRateLimit(request);
    signupRateLimitService.insertOrUpdateIpRateLimit(httpServletRequest);
  }

  private boolean isEmailUniqueViolation(
          DataIntegrityViolationException exception
  ) {
    Throwable cause = exception;

    while (cause != null) {
      if (cause instanceof org.hibernate.exception.ConstraintViolationException hibernateException) {
        return "uq_app_user_email_normalized"
                .equals(hibernateException.getConstraintName());
      }

      cause = cause.getCause();
    }

    return false;
  }

  public void validatePassword(String password) {
    PasswordPolicy.validate(password);
  }

  @Override
  public void signIn(SignInRequest signInRequest, HttpServletRequest request, HttpServletResponse response) {
    if(signInRequest == null || StringUtils.isBlank(signInRequest.getEmail()) || StringUtils.isBlank(signInRequest.getPassword())) {
        throw new IllegalArgumentException();
    }

    signInRateLimitService.checkIpRateLimit(request);

    UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(
            signInRequest.getEmail().trim().toLowerCase(Locale.ROOT), signInRequest.getPassword());

    Authentication authentication;
    try {
      authentication = authenticationManager.authenticate(token);
    } catch (org.springframework.security.core.AuthenticationException exception) {
      signInRateLimitService.recordFailedAuthentication(request);
      throw new AuthenticationException();
    }

    SecurityContext context = securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication);
    securityContextHolderStrategy.setContext(context);
    securityContextRepository.saveContext(context, request, response);

  }
}
