package com.appsdeveloperblog.ws.urlshorten.url.model.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUrlResponse {
  private String shortCode;
  private String shortUrl;
  private String status;
  private String validFrom;
  private String expiresAt;
}
