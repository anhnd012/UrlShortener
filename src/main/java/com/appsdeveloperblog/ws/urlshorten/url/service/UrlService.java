package com.appsdeveloperblog.ws.urlshorten.url.service;

import com.appsdeveloperblog.ws.urlshorten.url.model.request.CreateUrlRequest;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.CreateUrlResponse;

public interface UrlService {
    CreateUrlResponse createShortUrl(CreateUrlRequest request);
}
