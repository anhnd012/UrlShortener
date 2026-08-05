package com.appsdeveloperblog.ws.urlshorten.url.model.response;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.UUID;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ShortUrlAnalyticResponse {
    private UUID shortUrlId;
    private Long numberOfClicks;

    public ShortUrlAnalyticResponse(ShortUrl url) {
        this.shortUrlId = url.getId();
        this.numberOfClicks = url.getNumberOfClicks();

    }
}
