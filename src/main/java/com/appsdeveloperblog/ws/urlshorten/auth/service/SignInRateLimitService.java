package com.appsdeveloperblog.ws.urlshorten.auth.service;

import jakarta.servlet.http.HttpServletRequest;

public interface SignInRateLimitService {
    void checkIpRateLimit(HttpServletRequest httpServletRequest);

    void recordFailedAuthentication(HttpServletRequest httpServletRequest);
}
