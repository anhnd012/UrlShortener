package com.appsdeveloperblog.ws.urlshorten.url.messaging;

import com.appsdeveloperblog.ws.urlshorten.url.event.ClickUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RedisClickUpdatePublisher {
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  @Value("${app.redis.click-channel}")
  private String channel;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ClickUpdatedEvent event) {
    // Send event to Redis Pub/Sub
    try {
      String message = objectMapper.writeValueAsString(event);
      redisTemplate.convertAndSend(channel, message);
    } catch (JacksonException e) {
      throw new RuntimeException(e);
    }
  }
}
