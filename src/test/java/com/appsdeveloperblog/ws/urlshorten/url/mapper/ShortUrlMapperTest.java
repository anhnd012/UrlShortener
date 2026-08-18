package com.appsdeveloperblog.ws.urlshorten.url.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.event.ClickUpdatedEvent;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ListUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.UrlResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ShortUrlMapperTest {

  private ShortUrlMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new ShortUrlMapper();
    ReflectionTestUtils.setField(mapper, "baseUrl", "https://short.ly");
  }

  @Test
  void mappingUrlResponseMapsEntityFields() {
    Instant validFrom = Instant.parse("2026-08-15T00:00:00Z");
    Instant expiresAt = Instant.parse("2026-09-15T00:00:00Z");
    Instant createdAt = Instant.parse("2026-08-14T00:00:00Z");
    Instant updatedAt = Instant.parse("2026-08-15T01:00:00Z");
    ShortUrl shortUrl = createShortUrl(validFrom, expiresAt, createdAt, updatedAt);

    UrlResponse response = mapper.mappingUrlResponse(shortUrl);

    assertAll(
        () -> assertEquals("https://short.ly/urls/aB12xYz9", response.getShortUrl()),
        () -> assertEquals("https://example.com/article", response.getLongUrl()),
        () -> assertEquals("ACTIVE", response.getStatus()),
        () -> assertEquals(validFrom.toString(), response.getValidFrom()),
        () -> assertEquals(expiresAt.toString(), response.getExpiresAt()),
        () -> assertEquals(7L, response.getNumberOfClicks()),
        () -> assertEquals(createdAt.toString(), response.getCreatedAt()));
  }

  @Test
  void mappingListUrlResponseMapsEveryEntity() {
    ShortUrl shortUrl =
        createShortUrl(
            Instant.parse("2026-08-15T00:00:00Z"),
            Instant.parse("2026-09-15T00:00:00Z"),
            Instant.parse("2026-08-14T00:00:00Z"),
            Instant.parse("2026-08-15T01:00:00Z"));

    ListUrlResponse response = mapper.mappingListUrlResponse(List.of(shortUrl));

    assertEquals(1, response.getUrls().size());
    assertEquals("https://short.ly/urls/aB12xYz9", response.getUrls().get(0).getShortUrl());
  }

  @Test
  void mappingListUrlResponseWithNoEntitiesReturnsEmptyList() {
    ListUrlResponse response = mapper.mappingListUrlResponse(List.of());

    assertTrue(response.getUrls().isEmpty());
  }

  @Test
  void toClickUpdatedEventMapsClickInformation() {
    Instant updatedAt = Instant.parse("2026-08-15T01:00:00Z");
    ShortUrl shortUrl =
        createShortUrl(
            Instant.parse("2026-08-15T00:00:00Z"),
            Instant.parse("2026-09-15T00:00:00Z"),
            Instant.parse("2026-08-14T00:00:00Z"),
            updatedAt);

    ClickUpdatedEvent event = mapper.toClickUpdatedEvent(shortUrl);

    assertAll(
        () -> assertEquals(7L, event.numberOfClicks()),
        () -> assertEquals("aB12xYz9", event.shortCode()),
        () -> assertEquals(updatedAt, event.occuredAt()));
  }

  private ShortUrl createShortUrl(
      Instant validFrom, Instant expiresAt, Instant createdAt, Instant updatedAt) {
    ShortUrl shortUrl = new ShortUrl();
    shortUrl.setShortCode("aB12xYz9");
    shortUrl.setLongUrl("https://example.com/article");
    shortUrl.setStatus(UrlStatus.ACTIVE);
    shortUrl.setValidFrom(validFrom);
    shortUrl.setExpiresAt(expiresAt);
    shortUrl.setNumberOfClicks(7L);
    ReflectionTestUtils.setField(shortUrl, "createdAt", createdAt);
    ReflectionTestUtils.setField(shortUrl, "updatedAt", updatedAt);
    return shortUrl;
  }
}
