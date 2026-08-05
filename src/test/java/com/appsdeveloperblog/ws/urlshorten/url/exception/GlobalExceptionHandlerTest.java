package com.appsdeveloperblog.ws.urlshorten.url.exception;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleShortUrlRetryErrorReturnsInternalServerError() {
    UrlRetryException exception = new UrlRetryException("https://example.com/article");

    ResponseEntity<String> response = handler.handleShortUrlRetryError(exception);

    assertAll(
        () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode()),
        () -> assertEquals(exception.getMessage(), response.getBody()));
  }

  @Test
  void handleInvalidUrlErrorReturnsBadRequest() {
    InvalidUrlException exception = new InvalidUrlException("invalid url");

    ResponseEntity<String> response = handler.handleInvalidUrlError(exception);

    assertAll(
        () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
        () -> assertEquals(exception.getMessage(), response.getBody()));
  }

  @Test
  void handleConstraintViolationReturnsBadRequest() {
    ConstraintViolationException exception =
        new ConstraintViolationException("invalid path", Set.of());

    ResponseEntity<String> response = handler.handleConstraintViolation(exception);

    assertAll(
        () -> assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode()),
        () -> assertEquals("invalid path", response.getBody()));
  }
}
