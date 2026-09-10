package com.appsdeveloperblog.ws.urlshorten.url.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;
import com.appsdeveloperblog.ws.urlshorten.url.exception.InvalidUrlException;
import com.appsdeveloperblog.ws.urlshorten.url.exception.UrlRetryException;
import com.appsdeveloperblog.ws.urlshorten.url.mapper.ShortUrlMapper;
import com.appsdeveloperblog.ws.urlshorten.url.messaging.ClickEventPublisher;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ListUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ShortUrlAnalyticResponse;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import com.appsdeveloperblog.ws.urlshorten.url.service.RedirectCacheService;
import com.appsdeveloperblog.ws.urlshorten.url.service.UrlService;
import com.appsdeveloperblog.ws.urlshorten.url.util.ShortUrlUtil;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.appsdeveloperblog.ws.urlshorten.workplace.model.entity.Workplace;
import com.appsdeveloperblog.ws.urlshorten.workplace.repository.WorkplaceRepository;

@Service
@RequiredArgsConstructor
public class UrlServiceImpl implements UrlService {
  private final UrlRepository urlRepository;
  private final UserRepository userRepository;
  private final WorkplaceRepository workplaceRepository;
  private static final Integer MAX_ALLOWED_CHARACTERS = 2048;
  private static final String BASE62 =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final int MAX_RETRY = 5;
  private final RedirectCacheService redirectCacheService;
  private final ClickEventPublisher clickEventPublisher;
  private final ShortUrlMapper shortUrlMapper;

  @Value("${app.base-url}")
  private String baseUrl;

  @Override
  public CreateUrlResponse createShortUrl(CreateUrlRequest request) {
    String normalizedLongUrl = validateAndNormalize(request);
    for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
      String shortCode = generateRandomBase62(8);

      try {
        ShortUrl entity = buildShortUrlEntity(shortCode, normalizedLongUrl, request.getWorkplaceId());
        String shortUrl = ShortUrlUtil.buildShortUrl(baseUrl, shortCode);
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

  private ShortUrl buildShortUrlEntity(String shortCode, String normalizedLongUrl, UUID workplaceId) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(30, ChronoUnit.DAYS);

    User owner = getOrCreateDefaultOwner();
    Workplace workplace = resolveWorkplace(workplaceId, owner.getId());

    ShortUrl entity = new ShortUrl();
    entity.setShortCode(shortCode);
    entity.setLongUrl(normalizedLongUrl);
    entity.setStatus(UrlStatus.ACTIVE);
    entity.setValidFrom(now);
    entity.setExpiresAt(expiresAt);
    entity.setNumberOfClicks(0L);
    entity.setOwner(owner);
    entity.setWorkplace(workplace);
    return entity;
  }

  private Workplace resolveWorkplace(UUID requestedWorkplaceId, UUID ownerId) {
    if (requestedWorkplaceId != null) {
      return workplaceRepository.findById(requestedWorkplaceId).orElse(null);
    }

    List<Workplace> userWorkplaces = workplaceRepository.findAllByMemberUserId(ownerId);
    if (!userWorkplaces.isEmpty()) {
      return userWorkplaces.get(0);
    }

    List<Workplace> allWorkplaces = workplaceRepository.findAll();
    return allWorkplaces.isEmpty() ? null : allWorkplaces.get(0);
  }

  private User getOrCreateDefaultOwner() {
    return userRepository
        .findAll()
        .stream()
        .findFirst()
        .orElseGet(
            () -> {
              User defaultUser = new User();
              defaultUser.setEmail("user@example.com");
              defaultUser.setPasswordHash("$2a$10$defaultPasswordHashPlaceholder1234567890");
              defaultUser.setName("LinkFlow User");
              return userRepository.saveAndFlush(defaultUser);
            });
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
    ShortUrlClickedEvent clickedEvent =
        new ShortUrlClickedEvent(UUID.randomUUID(), shortCode, Instant.now(), 1);
    if (cachedLongUrl.isPresent()) {
      clickEventPublisher.publish(clickedEvent);
      return cachedLongUrl;
    }
    ShortUrl shortUrl = urlRepository.getByShortCode(shortCode);

    if (shortUrl == null
        || shortUrl.getStatus() != UrlStatus.ACTIVE
        || !shortUrl.getExpiresAt().isAfter(Instant.now())) {
      return Optional.empty();
    }
    redirectCacheService.put(
        shortCode, shortUrl.getLongUrl(), shortUrl.getExpiresAt().toEpochMilli());
    clickEventPublisher.publish(clickedEvent);
    return Optional.of(shortUrl.getLongUrl());
  }

  @Override
  public ShortUrlAnalyticResponse getAnalyticShortUrl(UUID urlId) {
    ShortUrl response = urlRepository.getById(urlId);
    return new ShortUrlAnalyticResponse(response);
  }

  @Override
  public ListUrlResponse getValidShortUrls(Integer pageNumber, Integer pageSize) {
    Instant now = Instant.now();
    Pageable pageable = PageRequest.of(pageNumber, pageSize);
    Page<ShortUrl> shortUrlList = urlRepository.getValidShortUrls(now, pageable);
    if (shortUrlList.getTotalElements() == 0) {
      return new ListUrlResponse();
    }
    ListUrlResponse response = shortUrlMapper.mappingListUrlResponse(shortUrlList.getContent());
    return response;
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public int expireOutdatedUrls() {
    Instant now = Instant.now();
    return urlRepository.expireOutdatedUrls(now);
  }
}
