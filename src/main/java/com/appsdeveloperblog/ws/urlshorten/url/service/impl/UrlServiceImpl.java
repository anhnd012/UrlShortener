package com.appsdeveloperblog.ws.urlshorten.url.service.impl;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.exception.InvalidUrlException;
import com.appsdeveloperblog.ws.urlshorten.url.exception.UrlRetryException;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import com.appsdeveloperblog.ws.urlshorten.url.service.RedirectCacheService;
import com.appsdeveloperblog.ws.urlshorten.url.service.UrlService;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {
  private final UrlRepository urlRepository;
  private final Integer MAX_ALLOWED_CHARACTERS = 2048;
  private static final String BASE62 =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private final int MAX_RETRY = 5;
  private final RedirectCacheService redirectCacheService;
  @Value("${app.base-url}")
  private String baseUrl;



  @Override
  public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
    String normalizedLongUrl = validateAndNormalize(request);
    for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
      String shortCode = generateRandomBase62(8);

      try {
        ShortUrl entity = buildShortUrlEntity(shortCode, normalizedLongUrl);
        String shortUrl = String.format("%s/%s", baseUrl, shortCode);
        urlRepository.saveAndFlush(entity);

        return CreateUrlResponse.builder()
            .shortCode(shortCode)
            .shortUrl(shortUrl)
            .status(UrlStatus.ACTIVE.name())
            .validFrom(entity.getValidFrom().toString())
            .expiresAt(entity.getExpiresAt().toString())
            .build();
      } catch (DataIntegrityViolationException ex) {
        if (attempt == MAX_RETRY) {
          throw new UrlRetryException(request.getLongUrl());
        }
      }
    }
    
    throw new UrlRetryException(request.getLongUrl());
  }

  private String normalizeUrl(String url) {
    String trimmedUrl = url.trim();
    if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
      return "https://" + trimmedUrl;
    }

    return trimmedUrl;
  }

  private boolean isValidUrl(URI uri) {
    String host = uri.getHost();
    if (!StringUtils.hasText(host) || !StringUtils.hasText(uri.getPath())) {
      return false;
    }

    if (host.equals("localhost")
        || host.equals("127.0.0.1")
        || host.startsWith("192.168.")
        || host.startsWith("10.")) {
      return false;
    }
    return true;
  }

  private String generateRandomBase62(int length) {
    StringBuffer sb = new StringBuffer();
    Random random = new Random();

    for (int i = 0; i < length; i++) {
      sb.append(BASE62.charAt(random.nextInt(BASE62.length())));
    }

    return sb.toString();
  }

  private ShortUrl buildShortUrlEntity(String shortCode, String normalizedLongUrl) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(30, ChronoUnit.DAYS);

    ShortUrl entity = new ShortUrl();
    entity.setShortCode(shortCode);
    entity.setLongUrl(normalizedLongUrl);
    entity.setStatus(UrlStatus.ACTIVE);
    entity.setValidFrom(now);
    entity.setExpiresAt(expiresAt);

    return entity;
  }

  private String validateAndNormalize(CreateUrlRequest request) {
    String normalizedLongUrl = normalizeUrl(request.getLongUrl());
    if (normalizedLongUrl.length() > MAX_ALLOWED_CHARACTERS) {
      throw new InvalidUrlException(request.getLongUrl());
    }

    try {
      URI uri = URI.create(normalizedLongUrl);
      if (!isValidUrl(uri)) {
        throw new InvalidUrlException(request.getLongUrl());
      }
    } catch (IllegalArgumentException e) {
      throw new InvalidUrlException(request.getLongUrl());
    }

    return normalizedLongUrl;
  }

  @Override
  public Optional<String> redirectShortUrl(String shortCode) {
    Optional<String> cachedLongUrl = redirectCacheService.getLongUrl(shortCode);
    if(cachedLongUrl.isPresent()) {
      return cachedLongUrl;
    }
    ShortUrl shortUrl = urlRepository.getByShortCode(shortCode);

    if (shortUrl == null
        || shortUrl.getStatus() != UrlStatus.ACTIVE
        || !shortUrl.getExpiresAt().isAfter(Instant.now())) {
      return Optional.empty();
    }
    redirectCacheService.put(shortCode, shortUrl.getLongUrl(), shortUrl.getExpiresAt().toEpochMilli());
    return Optional.of(shortUrl.getLongUrl());
  }
}
