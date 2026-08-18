package com.appsdeveloperblog.ws.urlshorten.url.entity;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessedClickEventTest {

  @Test
  void buildEntityCopiesEventIdentityAndSetsProcessedTime() {
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.parse("2026-08-15T00:00:00Z");
    ShortUrlClickedEvent event = new ShortUrlClickedEvent(eventId, "aB12xYz9", occurredAt, 1);

    ProcessedClickEvent processedEvent = new ProcessedClickEvent();
    processedEvent.buildEntity(event);

    assertAll(
        () -> assertEquals(eventId, processedEvent.getEventId()),
        () -> assertEquals("aB12xYz9", processedEvent.getShortCode()),
        () -> assertNotNull(processedEvent.getProcessedAt()));
  }
}
