package com.appsdeveloperblog.ws.urlshorten.url.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.common.lock.DistributedLockService;
import com.appsdeveloperblog.ws.urlshorten.url.service.UrlService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UrlExpirationSchedulerTest {

  private UrlService urlService;
  private DistributedLockService distributedLockService;
  private UrlExpirationScheduler scheduler;

  @BeforeEach
  void setUp() {
    urlService = mock(UrlService.class);
    distributedLockService = mock(DistributedLockService.class);
    scheduler = new UrlExpirationScheduler(urlService, distributedLockService);
    ReflectionTestUtils.setField(scheduler, "lockTtl", Duration.ofMinutes(10));
  }

  @Test
  void runDailyUrlExpirationJobExecutesWhenLockIsAcquired() {
    when(distributedLockService.acquireLock(anyString(), anyString(), any(Duration.class)))
        .thenReturn(true);
    when(urlService.expireOutdatedUrls()).thenReturn(5);

    scheduler.runDailyUrlExpirationJob();

    verify(distributedLockService).acquireLock(anyString(), anyString(), any(Duration.class));
    verify(urlService).expireOutdatedUrls();
    verify(distributedLockService).releaseLock(anyString(), anyString());
  }

  @Test
  void runDailyUrlExpirationJobSkipsWhenLockIsNotAcquired() {
    when(distributedLockService.acquireLock(anyString(), anyString(), any(Duration.class)))
        .thenReturn(false);

    scheduler.runDailyUrlExpirationJob();

    verify(distributedLockService).acquireLock(anyString(), anyString(), any(Duration.class));
    verify(urlService, never()).expireOutdatedUrls();
    verify(distributedLockService, never()).releaseLock(anyString(), anyString());
  }

  @Test
  void runDailyUrlExpirationJobReleasesLockEvenIfExceptionOccurs() {
    when(distributedLockService.acquireLock(anyString(), anyString(), any(Duration.class)))
        .thenReturn(true);
    doThrow(new RuntimeException("DB Connection Error")).when(urlService).expireOutdatedUrls();

    scheduler.runDailyUrlExpirationJob();

    verify(urlService).expireOutdatedUrls();
    verify(distributedLockService).releaseLock(anyString(), anyString());
  }
}
