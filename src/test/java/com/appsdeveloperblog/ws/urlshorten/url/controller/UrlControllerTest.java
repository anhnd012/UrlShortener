package com.appsdeveloperblog.ws.urlshorten.url.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.AccessTokenService;
import com.appsdeveloperblog.ws.urlshorten.common.exception.GlobalExceptionHandler;
import com.appsdeveloperblog.ws.urlshorten.config.SecurityConfig;
import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ListUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ShortUrlAnalyticResponse;
import com.appsdeveloperblog.ws.urlshorten.url.service.UrlService;
import io.jsonwebtoken.Jwts;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = UrlController.class, properties = "spring.config.name=controller-test")
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ImportAutoConfiguration({
  SecurityFilterAutoConfiguration.class,
  ServletWebSecurityAutoConfiguration.class
})
class UrlControllerTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID WORKPLACE_ID = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UrlService urlService;
  @MockitoBean private UserRepository users;
  @MockitoBean private AccessTokenService tokens;

  @BeforeEach
  void setUpAuthentication() {
    User user = new User();
    ReflectionTestUtils.setField(user, "id", USER_ID);
    user.setEmail("caller@example.com");
    when(tokens.verify("valid-token"))
        .thenReturn(Jwts.claims().subject(USER_ID.toString()).build());
    when(users.findWithRolesById(USER_ID)).thenReturn(Optional.of(user));
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createShortUrlWhenRequestIsValidReturnsCreatedResponse() throws Exception {
    CreateUrlResponse response =
        CreateUrlResponse.builder()
            .shortCode("aB12xYz9")
            .shortUrl("https://short.ly/urls/aB12xYz9")
            .status("ACTIVE")
            .validFrom("2026-06-14T08:00:00Z")
            .expiresAt("2026-07-14T08:00:00Z")
            .build();
    when(urlService.createShortUrl(any(CreateUrlRequest.class), eq(USER_ID), eq(WORKPLACE_ID)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/workplaces/{workplaceId}/urls", WORKPLACE_ID)
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "longUrl": "https://www.google.com/search?q=spring+boot"
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.shortCode").value("aB12xYz9"))
        .andExpect(jsonPath("$.shortUrl").value("https://short.ly/urls/aB12xYz9"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.validFrom").value("2026-06-14T08:00:00Z"))
        .andExpect(jsonPath("$.expiresAt").value("2026-07-14T08:00:00Z"));

    ArgumentCaptor<CreateUrlRequest> requestCaptor =
        ArgumentCaptor.forClass(CreateUrlRequest.class);
    verify(urlService).createShortUrl(requestCaptor.capture(), eq(USER_ID), eq(WORKPLACE_ID));
    assertEquals(
        "https://www.google.com/search?q=spring+boot", requestCaptor.getValue().getLongUrl());
  }

  @Test
  void createShortUrlWhenLongUrlIsBlankReturnsBadRequestAndDoesNotCallService() throws Exception {
    mockMvc
        .perform(
            post("/workplaces/{workplaceId}/urls", WORKPLACE_ID)
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "longUrl": ""
                                }
                                """))
        .andExpect(status().isBadRequest());

    verify(urlService, never())
        .createShortUrl(any(CreateUrlRequest.class), any(UUID.class), any(UUID.class));
  }

  @Test
  void createShortUrlWithoutAuthenticationIsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/workplaces/{workplaceId}/urls", WORKPLACE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\":\"https://example.com/article\"}"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(urlService);
  }

  @Test
  void createShortUrlWithMalformedWorkplaceIdDoesNotCallService() throws Exception {
    mockMvc
        .perform(
            post("/workplaces/not-a-uuid/urls")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\":\"https://example.com/article\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(urlService);
  }

  @Test
  void redirectShortUrlWhenShortCodeIsValidReturnsFoundWithLocationHeader() throws Exception {
    Optional<String> response = Optional.of("https://www.youtube.com/watch?v=spring");

    when(urlService.getValidLongUrl("aB12xYz9")).thenReturn(response);

    mockMvc
        .perform(get("/urls/aB12xYz9"))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, "https://www.youtube.com/watch?v=spring"));
  }

  @Test
  void redirectShortUrlWhenShortCodeIsInvalidReturnsNotFound() throws Exception {
    Optional<String> response = Optional.empty();

    when(urlService.getValidLongUrl("missing1")).thenReturn(response);

    mockMvc.perform(get("/urls/missing1")).andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"/urls", "/api/v1/urls", "/urls/00000000-0000-0000-0000-000000000001/analytics"})
  void managementEndpointsWithoutAuthenticationAreUnauthorized(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
    verifyNoInteractions(urlService);
  }

  @Test
  void apiCreateWithoutAuthenticationIsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"longUrl\":\"https://example.com/article\"}"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(urlService);
  }

  @Test
  void getAnalyticShortUrlReturnsOkResponse() throws Exception {
    UUID urlId = UUID.randomUUID();
    ShortUrlAnalyticResponse response =
        new ShortUrlAnalyticResponse(new com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl());
    when(urlService.getAnalyticShortUrl(urlId)).thenReturn(response);

    mockMvc
        .perform(
            get("/urls/{urlId}/analytics", urlId).header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk());

    verify(urlService).getAnalyticShortUrl(urlId);
  }

  @Test
  void getUrlsUsesDefaultPagination() throws Exception {
    ListUrlResponse response = new ListUrlResponse();
    when(urlService.getValidShortUrls(0, 10)).thenReturn(response);

    mockMvc
        .perform(get("/urls").header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk());

    verify(urlService).getValidShortUrls(0, 10);
  }
}
