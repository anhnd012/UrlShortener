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
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SpringBootTest(properties = "app.base-url=https://short.test")
@AutoConfigureMockMvc
class UrlApplicationIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Container
  static final KafkaContainer kafka =
      new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

  @DynamicPropertySource
  static void registerKafkaProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class KafkaTestConfig {

    @Bean
    NewTopic shortUrlClickedTopic() {
      return TopicBuilder.name("short-url-clicked").partitions(1).replicas(1).build();
    }
  }

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
        () -> assertEquals("https://short.test/urls/" + shortCode, shortUrl),
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
        .perform(get("/urls/{shortCode}", shortCode))
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
    mockMvc.perform(get("/urls/missing1")).andExpect(status().isNotFound());
    mockMvc.perform(get("/urls/short")).andExpect(status().isBadRequest());
  }

  @Test
  void expiredAndDisabledShortCodesReturnNotFound() throws Exception {
    urlRepository.saveAndFlush(
        shortUrl("expired1", UrlStatus.ACTIVE, Instant.now().minus(1, ChronoUnit.MINUTES), 1));
    urlRepository.saveAndFlush(
        shortUrl("disabled", UrlStatus.DISABLED, Instant.now().plus(1, ChronoUnit.DAYS), 1));

    mockMvc.perform(get("/urls/expired1")).andExpect(status().isNotFound());
    mockMvc.perform(get("/urls/disabled")).andExpect(status().isNotFound());
  }

  @Test
  void databaseRejectsDuplicateShortCodes() {
    urlRepository.saveAndFlush(
        shortUrl("duplcode", UrlStatus.ACTIVE, Instant.now().plus(1, ChronoUnit.DAYS), 1));

    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            urlRepository.saveAndFlush(
                shortUrl("duplcode", UrlStatus.ACTIVE, Instant.now().plus(1, ChronoUnit.DAYS), 1)));
  }

  private ShortUrl shortUrl(
      String shortCode, UrlStatus status, Instant expiresAt, long numberOfClicks) {
    ShortUrl shortUrl = new ShortUrl();
    shortUrl.setShortCode(shortCode);
    shortUrl.setLongUrl("https://example.com/articles/testing");
    shortUrl.setStatus(status);
    shortUrl.setValidFrom(Instant.now());
    shortUrl.setExpiresAt(expiresAt);
    shortUrl.setNumberOfClicks(numberOfClicks);
    return shortUrl;
  }
}
