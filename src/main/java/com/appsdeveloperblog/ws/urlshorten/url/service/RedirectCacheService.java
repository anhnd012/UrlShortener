package com.appsdeveloperblog.ws.urlshorten.url.service;

import java.util.Optional;

public interface RedirectCacheService {
  void put(String shortCode, String longUrl, long expiresAtMillis);

  Optional<String> getLongUrl(String shortCode);
}
