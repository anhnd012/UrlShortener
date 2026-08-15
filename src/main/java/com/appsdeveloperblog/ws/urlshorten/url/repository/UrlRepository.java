package com.appsdeveloperblog.ws.urlshorten.url.repository;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlRepository extends JpaRepository<ShortUrl, UUID> {

  @Query(
      """
        SELECT s
        FROM ShortUrl s
        WHERE s.shortCode = :shortCode
        """)
  ShortUrl getByShortCode(@Param("shortCode") String shortCode);

  @Query(
      """
        SELECT s
        FROM ShortUrl s
        WHERE s.id = :id
      """
  )
  ShortUrl getById(@Param("id") UUID id);

  @Modifying
  @Query(
    """
    UPDATE ShortUrl s
    SET s.numberOfClicks = s.numberOfClicks + 1
    WHERE s.shortCode = :shortCode
    """
  )
  int increasingShortUrlClicks(@Param("shortCode") String shortCode);

  @Query(
    """
    SELECT s
    FROM ShortUrl s
    WHERE s.status = 'ACTIVE'
    AND s.validFrom <= :now
    AND s.expiresAt > :now
    ORDER BY s.createdAt DESC
    """
  )
  Page<ShortUrl> getValidShortUrls(@Param("now")Instant now, Pageable pageable);


}
