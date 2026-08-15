package com.appsdeveloperblog.ws.urlshorten.url.mapper;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.event.ClickUpdatedEvent;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ListUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.UrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.util.ShortUrlUtil;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ShortUrlMapper {

  @Value("${app.base-url}")
  private String baseUrl;

  public ListUrlResponse mappingListUrlResponse(List<ShortUrl> urls) {
    ListUrlResponse response = new ListUrlResponse();
    List<UrlResponse> urlResponses = new ArrayList<>();
    for (ShortUrl shortUrl : urls) {
      UrlResponse urlResponse = mappingUrlResponse(shortUrl);
      urlResponses.add(urlResponse);
    }
    response.setUrls(urlResponses);
    return response;
  }

  public UrlResponse mappingUrlResponse(ShortUrl shortUrl) {
    UrlResponse response = new UrlResponse();
    response.setShortUrl(ShortUrlUtil.buildShortUrl(baseUrl, shortUrl.getShortCode()));
    response.setLongUrl(shortUrl.getLongUrl());
    response.setStatus(shortUrl.getStatus().toString());
    response.setClickNumbers(shortUrl.getNumberOfClicks());
    response.setValidFrom(shortUrl.getValidFrom().toString());
    response.setExpiresAt(shortUrl.getExpiresAt().toString());
    response.setCreatedAt(shortUrl.getCreatedAt().toString());
    return response;
  }

  public ClickUpdatedEvent toClickUpdatedEvent(ShortUrl shortUrl) {
    return new ClickUpdatedEvent(
        shortUrl.getNumberOfClicks(), shortUrl.getShortCode(), shortUrl.getUpdatedAt());
  }
}
