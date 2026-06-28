package com.appsdeveloperblog.ws.urlshorten.url.repository;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
