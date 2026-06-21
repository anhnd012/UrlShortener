package com.appsdeveloperblog.ws.urlshorten.url.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUrlRequest {

    @NotBlank
    String longUrl;
}
