package com.appsdeveloperblog.ws.urlshorten.url.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.exception.InvalidUrlException;
import com.appsdeveloperblog.ws.urlshorten.url.exception.UrlRetryException;
import com.appsdeveloperblog.ws.urlshorten.url.messaging.ClickEventPublisher;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import com.appsdeveloperblog.ws.urlshorten.url.service.RedirectCacheService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class UrlServiceImplTest {

  private UrlRepository urlRepository;
  private UrlServiceImpl urlService;
  private RedirectCacheService redirectCacheService;
  private ClickEventPublisher clickEventPublisher;

  @BeforeEach
  void setUp() {
    urlRepository = mock(UrlRepository.class);
    redirectCacheService = mock(RedirectCacheService.class);
    clickEventPublisher = mock(ClickEventPublisher.class);
    urlService = new UrlServiceImpl(urlRepository, redirectCacheService, clickEventPublisher, null);
    ReflectionTestUtils.setField(urlService, "baseUrl", "https://short.ly");
  }

  @Test
  void createShortUrlWhenLongUrlIsValidCreatesActiveShortUrlAndPersistsIt() {
    CreateUrlRequest request = createRequest("https://www.google.com/search?q=spring+boot");

    CreateUrlResponse response = urlService.createShortUrl(request);

    ArgumentCaptor<ShortUrl> shortUrlCaptor = ArgumentCaptor.forClass(ShortUrl.class);
    verify(urlRepository).saveAndFlush(shortUrlCaptor.capture());
    ShortUrl savedShortUrl = shortUrlCaptor.getValue();

    assertAll(
        () -> assertNotNull(response.getShortCode(), "shortCode should not be null"),
        () -> assertEquals(8, response.getShortCode().length(), "shortCode length should be 8"),
        () ->
            assertTrue(
                response.getShortCode().matches("[0-9a-zA-Z]{8}"),
                "shortCode should be Base62 with 8 chars"),
        () ->
            assertEquals(
                "https://short.ly/" + response.getShortCode(),
                response.getShortUrl(),
                "shortUrl should use base URL + shortCode"),
        () ->
            assertEquals(
                UrlStatus.ACTIVE.name(), response.getStatus(), "response status should be ACTIVE"),
        () ->
            assertEquals(
                response.getShortCode(),
                savedShortUrl.getShortCode(),
                "saved shortCode should match response shortCode"),
        () ->
            assertEquals(
                "https://www.google.com/search?q=spring+boot",
                savedShortUrl.getLongUrl(),
                "saved longUrl should match request"),
        () ->
            assertEquals(
                UrlStatus.ACTIVE, savedShortUrl.getStatus(), "saved status should be ACTIVE"),
        () -> assertNotNull(savedShortUrl.getValidFrom(), "validFrom should not be null"),
        () -> assertNotNull(savedShortUrl.getExpiresAt(), "expiresAt should not be null"),
        () ->
            assertEquals(
                savedShortUrl.getValidFrom().plus(30, ChronoUnit.DAYS),
                savedShortUrl.getExpiresAt(),
                "expiresAt should be validFrom + 30 days"),
        () ->
            assertEquals(
                savedShortUrl.getValidFrom(),
                Instant.parse(response.getValidFrom()),
                "response validFrom should match saved validFrom"),
        () ->
            assertEquals(
                savedShortUrl.getExpiresAt(),
                Instant.parse(response.getExpiresAt()),
                "response expiresAt should match saved expiresAt"));
  }

  @Test
  void createShortUrlWhenLongUrlHasNoProtocolNormalizesToHttpsBeforePersisting() {
    CreateUrlRequest request = createRequest("jakob.com/java-date-time");

    CreateUrlResponse response = urlService.createShortUrl(request);

    ArgumentCaptor<ShortUrl> shortUrlCaptor = ArgumentCaptor.forClass(ShortUrl.class);
    verify(urlRepository).saveAndFlush(shortUrlCaptor.capture());
    ShortUrl savedShortUrl = shortUrlCaptor.getValue();

    assertAll(
        () -> assertEquals("https://jakob.com/java-date-time", savedShortUrl.getLongUrl()),
        () -> assertEquals("https://short.ly/" + response.getShortCode(), response.getShortUrl()));
  }

  @Test
  void createShortUrlWhenLongUrlUsesHttpKeepsOriginalProtocol() {
    CreateUrlRequest request = createRequest("http://example.com/article");

    urlService.createShortUrl(request);

    ArgumentCaptor<ShortUrl> shortUrlCaptor = ArgumentCaptor.forClass(ShortUrl.class);
    verify(urlRepository).saveAndFlush(shortUrlCaptor.capture());
    assertEquals("http://example.com/article", shortUrlCaptor.getValue().getLongUrl());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://example.com",
        "http://localhost/article",
        "http://127.0.0.1/article",
        "http://192.168.1.10/article",
        "http://10.0.0.1/article"
      })
  void createShortUrlWhenUrlIsNotPublicOrHasNoPathRejectsIt(String longUrl) {
    CreateUrlRequest request = createRequest(longUrl);

    assertThrows(InvalidUrlException.class, () -> urlService.createShortUrl(request));

    verify(urlRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(ShortUrl.class));
  }

  @Test
  void createShortUrlWhenLongUrlIsInvalidThrowsExceptionAndDoesNotPersist() {
    CreateUrlRequest request = createRequest("abc def %%%");

    InvalidUrlException exception =
        assertThrows(InvalidUrlException.class, () -> urlService.createShortUrl(request));

    assertEquals("Url is invalid: " + request.getLongUrl(), exception.getMessage());
    verify(urlRepository, never()).save(org.mockito.ArgumentMatchers.any(ShortUrl.class));
  }

  @Test
  void createShortUrlWhenLongUrlExceedsMaximumLengthRejectsIt() {
    CreateUrlRequest request = createRequest("https://example.com/" + "a".repeat(2049));

    assertThrows(InvalidUrlException.class, () -> urlService.createShortUrl(request));

    verify(urlRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any(ShortUrl.class));
  }

  @Test
  void createShortUrlWhenShortCodeCollidesRetriesAndSucceeds() {
    CreateUrlRequest request = createRequest("https://example.com/articles/testing");
    when(urlRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(ShortUrl.class)))
        .thenThrow(new DataIntegrityViolationException("short code collision"))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CreateUrlResponse response = urlService.createShortUrl(request);

    assertNotNull(response);
    verify(urlRepository, times(2)).saveAndFlush(org.mockito.ArgumentMatchers.any(ShortUrl.class));
  }

  @Test
  void createShortUrlWhenEveryShortCodeCollidesThrowsRetryException() {
    CreateUrlRequest request = createRequest("https://example.com/articles/testing");
    when(urlRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(ShortUrl.class)))
        .thenThrow(new DataIntegrityViolationException("short code collision"));

    UrlRetryException exception =
        assertThrows(UrlRetryException.class, () -> urlService.createShortUrl(request));

    assertEquals(
        "Can not create short url based on: " + request.getLongUrl(), exception.getMessage());
    verify(urlRepository, times(5)).saveAndFlush(org.mockito.ArgumentMatchers.any(ShortUrl.class));
  }

  private CreateUrlRequest createRequest(String longUrl) {
    CreateUrlRequest request = new CreateUrlRequest();
    request.setLongUrl(longUrl);
    return request;
  }

  @Test
  void redirectShortUrlWhenShortCodeIsValidRedirectLongUrlApplication() {
    String shortCode = "aB12xYz9";
    ShortUrl shortUrl =
        createShortUrl(
            shortCode,
            "https://www.youtube.com/watch?v=spring",
            UrlStatus.ACTIVE,
            Instant.now().plus(30, ChronoUnit.DAYS));
    when(urlRepository.getByShortCode(shortCode)).thenReturn(shortUrl);

    Optional<String> response = urlService.redirectShortUrl(shortCode);

    assertAll(() -> assertEquals("https://www.youtube.com/watch?v=spring", response.get()));
  }

  @Test
  void redirectShortUrlWhenCacheContainsUrlDoesNotQueryDatabase() {
    String shortCode = "aB12xYz9";
    when(redirectCacheService.getLongUrl(shortCode))
        .thenReturn(Optional.of("https://example.com/cached"));

    Optional<String> response = urlService.redirectShortUrl(shortCode);

    assertEquals(Optional.of("https://example.com/cached"), response);
    verify(urlRepository, never()).getByShortCode(shortCode);
  }

  @Test
  void redirectShortUrlWhenShortCodeDoesNotExistReturnsInvalid() {
    String shortCode = "notFound";
    when(urlRepository.getByShortCode(shortCode)).thenReturn(null);

    Optional<String> response = urlService.redirectShortUrl(shortCode);

    assertAll(() -> assertTrue(response.isEmpty()));
  }

  @Test
  void redirectShortUrlWhenShortUrlIsExpiredReturnsInvalid() {
    String shortCode = "old12345";
    ShortUrl shortUrl =
        createShortUrl(
            shortCode,
            "https://www.youtube.com/watch?v=spring",
            UrlStatus.ACTIVE,
            Instant.now().minus(1, ChronoUnit.SECONDS));
    when(urlRepository.getByShortCode(shortCode)).thenReturn(shortUrl);

    Optional<String> response = urlService.redirectShortUrl(shortCode);

    assertTrue(response.isEmpty());
  }

  @Test
  void redirectShortUrlWhenShortUrlIsDisabledReturnsInvalid() {
    String shortCode = "off12345";
    ShortUrl shortUrl =
        createShortUrl(
            shortCode,
            "https://www.youtube.com/watch?v=spring",
            UrlStatus.DISABLED,
            Instant.now().plus(30, ChronoUnit.DAYS));
    when(urlRepository.getByShortCode(shortCode)).thenReturn(shortUrl);

    Optional<String> response = urlService.redirectShortUrl(shortCode);

    assertTrue(response.isEmpty());
  }

  private ShortUrl createShortUrl(
      String shortCode, String longUrl, UrlStatus status, Instant expiresAt) {
    ShortUrl shortUrl = new ShortUrl();
    shortUrl.setShortCode(shortCode);
    shortUrl.setLongUrl(longUrl);
    shortUrl.setStatus(status);
    shortUrl.setValidFrom(Instant.now());
    shortUrl.setExpiresAt(expiresAt);

    return shortUrl;
  }
}
