package com.appsdeveloperblog.ws.urlshorten.url.exception;

public class ShortUrlNotFoundException extends RuntimeException {
  public ShortUrlNotFoundException(String message) {
    super(message);
  }
}
