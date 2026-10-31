package com.appsdeveloperblog.ws.urlshorten.url.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.support.IntegrationContainersConfig;
import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;
import com.appsdeveloperblog.ws.urlshorten.url.exception.InvalidUrlException;
import com.appsdeveloperblog.ws.urlshorten.url.exception.ShortUrlNotFoundException;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@Import(IntegrationContainersConfig.class)
@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class ShortUrlClickedConsumerIT {

  @Autowired private ShortUrlClickedConsumer consumer;
  @Autowired private UrlRepository urlRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserRepository users;

  @BeforeEach
  void cleanDatabase() {
    jdbcTemplate.update("DELETE FROM processed_click_event");
    urlRepository.deleteAllInBatch();
  }

  @Test
  void redeliveredEventDoesNotIncrementClicksTwice() {
    String shortCode = "dedup123";
    urlRepository.saveAndFlush(shortUrl(shortCode));
    ShortUrlClickedEvent event = clickEvent(shortCode);

    consumer.consume(event);
    consumer.consume(event);

    assertEquals(1L, urlRepository.getByShortCode(shortCode).getNumberOfClicks());
    assertEquals(
        1,
        jdbcTemplate.queryForObject("SELECT count(*) FROM processed_click_event", Integer.class));
  }

  @Test
  void missingUrlRollsBackEventSoItCanBeRetried() {
    ShortUrlClickedEvent event = clickEvent("missing1");

    assertThrows(ShortUrlNotFoundException.class, () -> consumer.consume(event));
    assertEquals(
        0,
        jdbcTemplate.queryForObject("SELECT count(*) FROM processed_click_event", Integer.class));

    urlRepository.saveAndFlush(shortUrl(event.shortCode()));
    consumer.consume(event);
    assertEquals(1L, urlRepository.getByShortCode(event.shortCode()).getNumberOfClicks());
  }

  @Test
  void unsupportedEventVersionDoesNotPersistOrIncrementClicks() {
    String shortCode = "version1";
    urlRepository.saveAndFlush(shortUrl(shortCode));
    var event = new ShortUrlClickedEvent(UUID.randomUUID(), shortCode, Instant.now(), 2);

    assertThrows(InvalidUrlException.class, () -> consumer.consume(event));
    assertEquals(0L, urlRepository.getByShortCode(shortCode).getNumberOfClicks());
    assertEquals(
        0,
        jdbcTemplate.queryForObject("SELECT count(*) FROM processed_click_event", Integer.class));
  }

  @Test
  void concurrentClickEventsIncrementWithoutLostUpdates() throws Exception {
    String shortCode = "abc12345";
    int incrementCount = 200;
    urlRepository.saveAndFlush(shortUrl(shortCode));

    consumeConcurrently(
        IntStream.range(0, incrementCount).mapToObj(index -> clickEvent(shortCode)).toList());

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

  @Test
  void concurrentDeliveriesOfTheSameEventOnlyCountOnce() throws Exception {
    String shortCode = "samevent";
    urlRepository.saveAndFlush(shortUrl(shortCode));
    ShortUrlClickedEvent event = clickEvent(shortCode);

    consumeConcurrently(Collections.nCopies(20, event));

    assertEquals(1L, urlRepository.getByShortCode(shortCode).getNumberOfClicks());
    assertEquals(
        1,
        jdbcTemplate.queryForObject("SELECT count(*) FROM processed_click_event", Integer.class));
  }

  private void consumeConcurrently(List<ShortUrlClickedEvent> events) throws Exception {
    int workerCount = 10;

    ExecutorService executor = Executors.newFixedThreadPool(workerCount);
    CountDownLatch startGate = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();

    try {
      for (ShortUrlClickedEvent event : events) {
        futures.add(
            executor.submit(
                () -> {
                  startGate.await();
                  consumer.consume(event);
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
  }

  private ShortUrl shortUrl(String shortCode) {
    ShortUrl shortUrl = new ShortUrl();
    shortUrl.setShortCode(shortCode);
    shortUrl.setLongUrl("https://example.com/article");
    shortUrl.setStatus(UrlStatus.ACTIVE);
    shortUrl.setValidFrom(Instant.now());
    shortUrl.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
    shortUrl.setNumberOfClicks(0L);
    shortUrl.setOwner(users.findByEmail("bootstrap@example.test").orElseThrow());
    return shortUrl;
  }

  private ShortUrlClickedEvent clickEvent(String shortCode) {
    return new ShortUrlClickedEvent(UUID.randomUUID(), shortCode, Instant.now(), 1);
  }
}
