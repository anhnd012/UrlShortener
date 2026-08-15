package com.appsdeveloperblog.ws.urlshorten.url.repository;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ProcessedClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedClickEventRepository extends JpaRepository<ProcessedClickEvent, UUID> {
    @Modifying
    @Query(
    """
    INSERT INTO ProcessedClickEvent(eventId, shortCode, processedAt) 
    VALUES(:event_id, :short_code, :processed_at)   
    """
    )
    int increasingClickEvents(@Param("event_id")UUID eventId,
                              @Param("short_code")String shortCode,
                              @Param("processed_at")Instant processedAt);
}
