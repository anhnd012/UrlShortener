package com.appsdeveloperblog.ws.urlshorten.auth.service;

public interface RedisService {
  void saveRefreshToken(String refreshToken, long expInSeconds);
}
