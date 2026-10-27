package com.appsdeveloperblog.ws.urlshorten.auth.service;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import java.util.UUID;

public interface PasswordResetTokenService {

  String createToken(User user);

  UUID consumeToken(String rawToken);

  String createRandomRefreshToken();
}
