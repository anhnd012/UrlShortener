package com.appsdeveloperblog.ws.urlshorten.url.repository;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ProcessedClickEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcessedClickEventRepository extends JpaRepository<ProcessedClickEvent, UUID> {
  @Modifying
  @Query(
      value =
          """
          INSERT INTO processed_click_event(event_id, short_code, processed_at)
          VALUES(:event_id, :short_code, :processed_at)
          ON CONFLICT (event_id) DO NOTHING
          """,
      nativeQuery = true)
  int increasingClickEvents(
      @Param("event_id") UUID eventId,
      @Param("short_code") String shortCode,
      @Param("processed_at") Instant processedAt);
}
