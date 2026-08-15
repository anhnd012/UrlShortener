package com.appsdeveloperblog.ws.urlshorten.url.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Response payload containing a list of URL response details")
public class ListUrlResponse {
  @Schema(description = "List of URL response items")
  private List<UrlResponse> urls;
}
