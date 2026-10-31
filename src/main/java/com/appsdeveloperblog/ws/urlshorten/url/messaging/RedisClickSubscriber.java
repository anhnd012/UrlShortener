package com.appsdeveloperblog.ws.urlshorten.url.messaging;

import com.appsdeveloperblog.ws.urlshorten.url.event.ClickUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisClickSubscriber implements MessageListener {
  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      ClickUpdatedEvent event = objectMapper.readValue(message.getBody(), ClickUpdatedEvent.class);
      log.info(
          "Received ClickUpdatedEvent from Redis: {}. Forwarding to STOMP /topic/links", event);
      messagingTemplate.convertAndSend("/topic/links", event);
    } catch (Exception ex) {
      log.error(
          "Cannot deserialize ClickUpdatedEvent from message body: "
              + new String(message.getBody()),
          ex);
    }
  }
}
