package com.appsdeveloperblog.ws.urlshorten.url.controller;

import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.RedirectShortUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.service.UrlService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UrlController.class)
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @Test
    void createShortUrlWhenRequestIsValidReturnsCreatedResponse() throws Exception {
        CreateUrlResponse response = CreateUrlResponse.builder()
                .shortCode("aB12xYz9")
                .shortUrl("https://short.ly/aB12xYz9")
                .status("ACTIVE")
                .validFrom("2026-06-14T08:00:00Z")
                .expiresAt("2026-07-14T08:00:00Z")
                .build();
        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "longUrl": "https://www.google.com/search?q=spring+boot"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("aB12xYz9"))
                .andExpect(jsonPath("$.shortUrl").value("https://short.ly/aB12xYz9"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.validFrom").value("2026-06-14T08:00:00Z"))
                .andExpect(jsonPath("$.expiresAt").value("2026-07-14T08:00:00Z"));

        ArgumentCaptor<CreateUrlRequest> requestCaptor = ArgumentCaptor.forClass(CreateUrlRequest.class);
        verify(urlService).createShortUrl(requestCaptor.capture());
        assertEquals(
                "https://www.google.com/search?q=spring+boot",
                requestCaptor.getValue().getLongUrl()
        );
    }

    @Test
    void createShortUrlWhenLongUrlIsBlankReturnsBadRequestAndDoesNotCallService() throws Exception {
        mockMvc.perform(post("/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "longUrl": ""
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(urlService, never()).createShortUrl(any(CreateUrlRequest.class));
    }

    @Test
    void redirectShortUrlWhenShortCodeIsValidReturnsFoundWithLocationHeader() throws Exception {
        Optional<String> response = Optional.of("https://www.youtube.com/watch?v=spring");

        when(urlService.redirectShortUrl("aB12xYz9")).thenReturn(response);

        mockMvc.perform(get("/aB12xYz9"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://www.youtube.com/watch?v=spring"));
    }

    @Test
    void redirectShortUrlWhenShortCodeIsInvalidReturnsNotFound() throws Exception {
        Optional<String> response = Optional.empty();

        when(urlService.redirectShortUrl("missing1")).thenReturn(response);

        mockMvc.perform(get("/missing1"))
                .andExpect(status().isNotFound());
    }
}
