package com.appsdeveloperblog.ws.urlshorten.url.scheduler;

import com.appsdeveloperblog.ws.urlshorten.common.lock.DistributedLockService;
import com.appsdeveloperblog.ws.urlshorten.url.service.UrlService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlExpirationScheduler {

  private static final String LOCK_KEY = "lock:job:url-expiration";

  private final UrlService urlService;
  private final DistributedLockService distributedLockService;

  @Value("${app.scheduler.lock-at-most-for:10m}")
  private Duration lockTtl;

  /**
   * Daily background job running at midnight (00:00:00) to update expired URLs from ACTIVE to EXPIRED.
   * Uses a Redis Distributed Lock so that in a multi-instance cluster, only 1 instance executes the job.
   */
  @Scheduled(cron = "${app.scheduler.expire-urls-cron:0 0 0 * * ?}")
  public void runDailyUrlExpirationJob() {
    String lockValue = UUID.randomUUID().toString();
    log.info("Triggered daily URL expiration job. Attempting to acquire lock '{}'...", LOCK_KEY);

    boolean lockAcquired = distributedLockService.acquireLock(LOCK_KEY, lockValue, lockTtl != null ? lockTtl : Duration.ofMinutes(10));
    if (!lockAcquired) {
      log.info("URL expiration job is already executing or ran on another instance (lock '{}' active). Skipping execution on this instance.", LOCK_KEY);
      return;
    }

    try {
      log.info("Acquired distributed lock successfully. Executing URL expiration...");
      int expiredCount = urlService.expireOutdatedUrls();
      log.info("URL expiration job completed. Successfully updated {} expired URLs to status EXPIRED.", expiredCount);
    } catch (Exception e) {
      log.error("Error occurred while executing daily URL expiration job: {}", e.getMessage(), e);
    } finally {
      distributedLockService.releaseLock(LOCK_KEY, lockValue);
    }
  }
}
