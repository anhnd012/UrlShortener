package com.appsdeveloperblog.ws.urlshorten.auth.service;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.SignUpRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface SignUpRateLimitService {
    void insertOrUpdateEmailRateLimit(SignUpRequest request);

    void insertOrUpdateIpRateLimit(HttpServletRequest httpServletRequest);
}
