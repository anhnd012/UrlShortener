package com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response;

import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, String username, Set<String> roles) {}
