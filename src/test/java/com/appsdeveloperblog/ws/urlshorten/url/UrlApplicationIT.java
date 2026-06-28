package com.appsdeveloperblog.ws.urlshorten.url;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "app.base-url=https://short.test")
@AutoConfigureMockMvc
class UrlApplicationIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private MockMvc mockMvc;
  @Autowired private UrlRepository urlRepository;

  @BeforeEach
  void cleanDatabase() {
    urlRepository.deleteAll();
  }

  @Test
  void createThenRedirectPersistsAndReturnsTheOriginalUrl() throws Exception {
    String longUrl = "https://www.google.com/search?q=spring+boot";

    MvcResult result =
        mockMvc
            .perform(
                post("/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"longUrl\":\"%s\"}".formatted(longUrl)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shortCode").isString())
            .andExpect(jsonPath("$.shortUrl").isString())
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.validFrom").isString())
            .andExpect(jsonPath("$.expiresAt").isString())
            .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    String shortCode = JsonPath.read(responseBody, "$.shortCode");
    String shortUrl = JsonPath.read(responseBody, "$.shortUrl");
    Instant validFrom = Instant.parse(JsonPath.read(responseBody, "$.validFrom"));
    Instant expiresAt = Instant.parse(JsonPath.read(responseBody, "$.expiresAt"));
    ShortUrl persisted = urlRepository.getByShortCode(shortCode);

    assertAll(
        () -> assertTrue(shortCode.matches("[0-9a-zA-Z]{8}")),
        () -> assertEquals("https://short.test/" + shortCode, shortUrl),
        () -> assertNotNull(persisted),
        () -> assertEquals(longUrl, persisted.getLongUrl()),
        () -> assertEquals(UrlStatus.ACTIVE, persisted.getStatus()),
        () ->
            assertTrue(
                Math.abs(ChronoUnit.NANOS.between(validFrom, persisted.getValidFrom())) <= 1_000),
        () ->
            assertTrue(
                Math.abs(ChronoUnit.NANOS.between(expiresAt, persisted.getExpiresAt())) <= 1_000),
        () -> assertEquals(validFrom.plus(30, ChronoUnit.DAYS), expiresAt));

    mockMvc
        .perform(get("/{shortCode}", shortCode))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, longUrl));
  }

  @Test
  void createWithoutProtocolNormalizesToHttpsBeforePersisting() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"longUrl\":\"example.com/articles/testing\"}"))
            .andExpect(status().isCreated())
            .andReturn();

    String shortCode = JsonPath.read(result.getResponse().getContentAsString(), "$.shortCode");

    assertEquals(
        "https://example.com/articles/testing",
        urlRepository.getByShortCode(shortCode).getLongUrl());
  }

  @Test
  void invalidCreateRequestsReturnBadRequestWithoutPersisting() throws Exception {
    mockMvc
        .perform(
            post("/urls").contentType(MediaType.APPLICATION_JSON).content("{\"longUrl\":\"\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\":\"abc def %%%\"}"))
        .andExpect(status().isBadRequest());

    String oversizedUrl = "https://example.com/" + "a".repeat(2049);
    mockMvc
        .perform(
            post("/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\":\"%s\"}".formatted(oversizedUrl)))
        .andExpect(status().isBadRequest());

    assertEquals(0, urlRepository.count());
  }

  @Test
  void missingAndMalformedShortCodesReturnClientErrors() throws Exception {
    mockMvc.perform(get("/missing1")).andExpect(status().isNotFound());
    mockMvc.perform(get("/short")).andExpect(status().isBadRequest());
  }

  @Test
  void expiredAndDisabledShortCodesReturnNotFound() throws Exception {
    urlRepository.saveAndFlush(
        shortUrl("expired1", UrlStatus.ACTIVE, Instant.now().minus(1, ChronoUnit.MINUTES)));
    urlRepository.saveAndFlush(
        shortUrl("disabled", UrlStatus.DISABLED, Instant.now().plus(1, ChronoUnit.DAYS)));

    mockMvc.perform(get("/expired1")).andExpect(status().isNotFound());
    mockMvc.perform(get("/disabled")).andExpect(status().isNotFound());
  }

  @Test
  void databaseRejectsDuplicateShortCodes() {
    urlRepository.saveAndFlush(
        shortUrl("duplcode", UrlStatus.ACTIVE, Instant.now().plus(1, ChronoUnit.DAYS)));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            urlRepository.saveAndFlush(
                shortUrl("duplcode", UrlStatus.ACTIVE, Instant.now().plus(1, ChronoUnit.DAYS))));
  }

  private ShortUrl shortUrl(String shortCode, UrlStatus status, Instant expiresAt) {
    ShortUrl shortUrl = new ShortUrl();
    shortUrl.setShortCode(shortCode);
    shortUrl.setLongUrl("https://example.com/articles/testing");
    shortUrl.setStatus(status);
    shortUrl.setValidFrom(Instant.now());
    shortUrl.setExpiresAt(expiresAt);
    return shortUrl;
  }
}
