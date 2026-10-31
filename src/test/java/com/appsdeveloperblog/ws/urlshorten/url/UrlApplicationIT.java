package com.appsdeveloperblog.ws.urlshorten.url;

import static org.awaitility.Awaitility.await;
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

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.AccessTokenService;
import com.appsdeveloperblog.ws.urlshorten.support.IntegrationContainersConfig;
import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.entity.Workplace;
import com.appsdeveloperblog.ws.urlshorten.workplace.repository.WorkplaceRepository;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
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
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@Import({IntegrationContainersConfig.class, UrlApplicationIT.KafkaTestConfig.class})
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=true")
@AutoConfigureMockMvc
class UrlApplicationIT {

  @TestConfiguration(proxyBeanMethods = false)
  static class KafkaTestConfig {

    @Bean
    @ServiceConnection
    KafkaContainer kafka() {
      return new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));
    }

    @Bean
    NewTopic shortUrlClickedTopic() {
      return TopicBuilder.name("short-url-clicked").partitions(1).replicas(1).build();
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private UrlRepository urlRepository;
  @Autowired private UserRepository users;
  @Autowired private WorkplaceRepository workplaces;
  @Autowired private AccessTokenService accessTokens;

  private User owner;
  private Workplace workplace;
  private String authorization;

  @BeforeEach
  void cleanDatabase() {
    urlRepository.deleteAllInBatch();
    workplaces.deleteAllInBatch();
    owner =
        users
            .findByEmail("url-owner@example.test")
            .orElseGet(
                () -> {
                  User user = new User();
                  user.setEmail("url-owner@example.test");
                  user.setPasswordHash("test-only-hash");
                  return users.saveAndFlush(user);
                });
    workplace = new Workplace();
    workplace.setName("Regression tests");
    workplace.setOwner(owner);
    workplace = workplaces.saveAndFlush(workplace);
    authorization = "Bearer " + accessTokens.create(owner.getId());
  }

  @Test
  void createThenRedirectPersistsAndReturnsTheOriginalUrl() throws Exception {
    String longUrl = "https://www.google.com/search?q=spring+boot";

    MvcResult result =
        mockMvc
            .perform(
                post("/workplaces/{workplaceId}/urls", workplace.getId())
                    .header(HttpHeaders.AUTHORIZATION, authorization)
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
        () -> assertEquals(owner.getId(), persisted.getOwner().getId()),
        () -> assertEquals(workplace.getId(), persisted.getWorkplace().getId()),
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

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> assertEquals(1L, urlRepository.getByShortCode(shortCode).getNumberOfClicks()));
  }

  @Test
  void createWithoutProtocolNormalizesToHttpsBeforePersisting() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/workplaces/{workplaceId}/urls", workplace.getId())
                    .header(HttpHeaders.AUTHORIZATION, authorization)
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
            post("/workplaces/{workplaceId}/urls", workplace.getId())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\":\"\"}"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/workplaces/{workplaceId}/urls", workplace.getId())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\":\"abc def %%%\"}"))
        .andExpect(status().isBadRequest());

    String oversizedUrl = "https://example.com/" + "a".repeat(2049);
    mockMvc
        .perform(
            post("/workplaces/{workplaceId}/urls", workplace.getId())
                .header(HttpHeaders.AUTHORIZATION, authorization)
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
  void unauthenticatedCreationDoesNotPersistUrl() throws Exception {
    mockMvc
        .perform(
            post("/workplaces/{workplaceId}/urls", workplace.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\":\"https://example.com/article\"}"))
        .andExpect(status().isUnauthorized());
    assertEquals(0, urlRepository.count());
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
    shortUrl.setOwner(owner);
    shortUrl.setWorkplace(workplace);
    return shortUrl;
  }
}
