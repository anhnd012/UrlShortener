package com.appsdeveloperblog.ws.urlshorten.url.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Detail information for a single shortened URL response")
@Data
public class UrlResponse {
  @Schema(description = "The original URL")
  private String longUrl;

  @Schema(
      description = "The complete shortened URL that redirects to the original URL",
      example = "http://short.ly/aBcdEfGh")
  private String shortUrl;

  @Schema(description = "The status of the shortened URL (e.g., ACTIVE)", example = "ACTIVE")
  private String status;

  @Schema(
      description = "The timestamp from which the shortened URL is valid",
      example = "2026-07-18T13:30:54Z")
  private String validFrom;

  @Schema(
      description = "The timestamp at which the shortened URL expires",
      example = "2026-08-18T13:30:54Z")
  private String expiresAt;

  @Schema(description = "The total short link's click number")
  private Integer clickNumbers;
}
