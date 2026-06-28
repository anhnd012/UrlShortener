package com.appsdeveloperblog.ws.urlshorten.url.exception;

public class InvalidUrlException extends RuntimeException {

  public InvalidUrlException(String longUrl) {
    super("Url is invalid: " + longUrl);
  }
}
