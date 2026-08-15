package com.appsdeveloperblog.ws.urlshorten.url.event;

import java.time.Instant;
import java.util.UUID;

public record ShortUrlClickedEvent(
    UUID eventId, String shortCode, Instant occurredAt, int version) {}
