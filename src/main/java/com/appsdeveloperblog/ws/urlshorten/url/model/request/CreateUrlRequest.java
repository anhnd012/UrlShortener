package com.appsdeveloperblog.ws.urlshorten.url.model.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateUrlRequest {

  @NotBlank(message = "longUrl must not be blank")
  private String longUrl;

  private UUID workplaceId;
}
