package com.appsdeveloperblog.ws.urlshorten.auth.service;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignInRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignUpRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthenticationService {
    void signUp(SignUpRequest request, HttpServletRequest httpServletRequest);
    void signIn(SignInRequest signInRequest, HttpServletRequest request, HttpServletResponse response);
}
