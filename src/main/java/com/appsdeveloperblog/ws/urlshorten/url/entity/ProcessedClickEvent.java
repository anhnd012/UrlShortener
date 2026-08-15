package com.appsdeveloperblog.ws.urlshorten.url.entity;

import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ProcessedClickEvent {
  @Id private UUID eventId;

  private String shortCode;

  private Instant processedAt;

  public void buildEntity(ShortUrlClickedEvent event) {
    this.eventId = event.eventId();
    this.shortCode = event.shortCode();
    this.processedAt = Instant.now();
  }
}
