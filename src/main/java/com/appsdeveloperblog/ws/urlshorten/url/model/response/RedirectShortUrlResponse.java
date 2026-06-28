package com.appsdeveloperblog.ws.urlshorten.url.model.response;

import lombok.Data;

@Data
public class RedirectShortUrlResponse {
    private String longUrl;
    private String status;
}
