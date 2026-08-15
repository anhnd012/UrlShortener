package com.appsdeveloperblog.ws.urlshorten.url.event;

import java.time.Instant;

public record ClickUpdatedEvent (
        long numberOfClicks,
        String shortCode,
        Instant occuredAt
){}
