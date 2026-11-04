package com.appsdeveloperblog.ws.urlshorten.auth.exception;

public class ExceedRateLimitException extends RuntimeException {

  public ExceedRateLimitException(String message) {
    super(message);
  }
}
