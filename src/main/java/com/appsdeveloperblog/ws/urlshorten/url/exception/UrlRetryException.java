package com.appsdeveloperblog.ws.urlshorten.url.exception;

public class UrlRetryException extends RuntimeException {

  public UrlRetryException(String longUrl) {
    super("Can not create short url based on: " + longUrl);
  }
}
