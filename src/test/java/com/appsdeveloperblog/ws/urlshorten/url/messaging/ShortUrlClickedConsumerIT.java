package com.appsdeveloperblog.ws.urlshorten.url.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class ShortUrlClickedConsumerIT {

  @Container @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private ShortUrlClickedConsumer consumer;
  @Autowired private UrlRepository urlRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void concurrentClickEventsIncrementWithoutLostUpdates() throws Exception {
    String shortCode = "abc12345";
    int incrementCount = 200;
    int workerCount = 10;
    urlRepository.saveAndFlush(shortUrl(shortCode));

    ExecutorService executor = Executors.newFixedThreadPool(workerCount);
    CountDownLatch startGate = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();

    try {
      for (int i = 0; i < incrementCount; i++) {
        futures.add(
            executor.submit(
                () -> {
                  startGate.await();
                  consumer.consume(clickEvent(shortCode));
                  return null;
                }));
      }

      startGate.countDown();

      for (Future<?> future : futures) {
        future.get(20, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
    }

    Long finalCount =
        jdbcTemplate.queryForObject(
            """
            SELECT number_of_clicks
            FROM short_url
            WHERE short_code = ?
            """,
            Long.class,
            shortCode);

    assertEquals((long) incrementCount, finalCount);
  }

  private ShortUrl shortUrl(String shortCode) {
    ShortUrl shortUrl = new ShortUrl();
    shortUrl.setShortCode(shortCode);
    shortUrl.setLongUrl("https://example.com/article");
    shortUrl.setStatus(UrlStatus.ACTIVE);
    shortUrl.setValidFrom(Instant.now());
    shortUrl.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
    shortUrl.setNumberOfClicks(0L);
    return shortUrl;
  }

  private ShortUrlClickedEvent clickEvent(String shortCode) {
    return new ShortUrlClickedEvent(UUID.randomUUID(), shortCode, Instant.now(), 1);
  }
}
