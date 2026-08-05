package com.appsdeveloperblog.ws.urlshorten.url.repository;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
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
}
