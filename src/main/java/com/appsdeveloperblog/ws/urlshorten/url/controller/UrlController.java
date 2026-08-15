package com.appsdeveloperblog.ws.urlshorten.url.controller;

import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ListUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ShortUrlAnalyticResponse;
import com.appsdeveloperblog.ws.urlshorten.url.service.UrlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
public class UrlController {
  private final UrlService urlService;

  @PostMapping({"/urls", "/api/v1/urls"})
  public ResponseEntity createShortUrl(@RequestBody @Valid CreateUrlRequest request) {
    CreateUrlResponse response = urlService.createShortUrl(request);
    return new ResponseEntity(response, HttpStatus.CREATED);
  }

  @GetMapping("/{shortCode}")
  public ResponseEntity<Void> redirectShortUrl(
      @PathVariable @NotBlank @Size(min = 8, max = 8) String shortCode) {
    return urlService
        .redirectShortUrl(shortCode)
        .map(
            longUrl ->
                ResponseEntity.status(HttpStatus.FOUND).location(URI.create(longUrl)).<Void>build())
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/urls/{urlId}/analytics")
  public ResponseEntity<ShortUrlAnalyticResponse> getAnalyticShortUrl(@PathVariable UUID urlId) {
    ShortUrlAnalyticResponse response = urlService.getAnalyticShortUrl(urlId);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @GetMapping({"/urls", "/api/v1/urls"})
  public ResponseEntity<ListUrlResponse> getUrls(
      @RequestParam(defaultValue = "0") Integer pageNumber,
      @RequestParam(defaultValue = "10") Integer pageSize) {
    ListUrlResponse response = urlService.getValidShortUrls(pageNumber, pageSize);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }
}
