package com.appsdeveloperblog.ws.urlshorten.auth.service;

import io.jsonwebtoken.Claims;
import java.util.UUID;

public interface AccessTokenService {
    String create(UUID userId);

    Claims verify(String rawToken);
}
