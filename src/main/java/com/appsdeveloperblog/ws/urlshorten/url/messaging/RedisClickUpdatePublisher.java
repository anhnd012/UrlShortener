package com.appsdeveloperblog.ws.urlshorten.url.messaging;

import com.appsdeveloperblog.ws.urlshorten.url.event.ClickUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisClickUpdatePublisher {
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.redis.click-channel}")
  private String channel;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void handle(ClickUpdatedEvent event) {
    try {
      String message = objectMapper.writeValueAsString(event);
      log.info("Publishing ClickUpdatedEvent to Redis channel '{}': {}", channel, message);
      redisTemplate.convertAndSend(channel, message);
    } catch (Exception e) {
      log.error("Failed to publish ClickUpdatedEvent to Redis channel: {}", e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }
}
