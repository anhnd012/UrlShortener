package com.appsdeveloperblog.ws.urlshorten.auth.controller;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.ForgotPasswordRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.ResetPasswordRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignInRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignUpRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.service.AuthenticationService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody @Valid SignUpRequest request,
                                       HttpServletRequest httpServletRequest) {
        authenticationService.signUp(request, httpServletRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/signin")
    public ResponseEntity<Void> signIn(@RequestBody @Valid SignInRequest signInRequest,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        authenticationService.signIn(signInRequest, request, response);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request,
            HttpServletRequest httpServletRequest) {
        passwordResetService.requestReset(request, httpServletRequest);
        return ResponseEntity.accepted()
                .body("If an account exists for this email, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}

