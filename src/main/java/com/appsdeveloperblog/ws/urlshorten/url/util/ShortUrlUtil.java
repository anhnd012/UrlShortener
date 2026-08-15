package com.appsdeveloperblog.ws.urlshorten.url.util;

public class ShortUrlUtil {

  public static String buildShortUrl(String baseUrl, String shortCode) {
    return String.format("%s/%s", baseUrl, shortCode);
  }
}
