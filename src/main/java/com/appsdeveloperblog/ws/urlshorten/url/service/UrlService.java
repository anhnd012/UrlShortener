package com.appsdeveloperblog.ws.urlshorten.url.service;

import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ListUrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.ShortUrlAnalyticResponse;

import java.util.Optional;
import java.util.UUID;

public interface UrlService {
  CreateUrlResponse createShortUrl(CreateUrlRequest request);

  Optional<String> redirectShortUrl(String shortCode);
  ShortUrlAnalyticResponse getAnalyticShortUrl(UUID urlId);
  ListUrlResponse getValidShortUrls(Integer pageNumber,Integer pageSize);

}
