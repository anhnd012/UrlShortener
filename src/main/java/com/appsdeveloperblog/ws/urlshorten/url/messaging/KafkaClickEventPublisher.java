package com.appsdeveloperblog.ws.urlshorten.url.messaging;

import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaClickEventPublisher implements ClickEventPublisher {
  private final KafkaTemplate<String, ShortUrlClickedEvent> kafkaTemplate;

  @Value("${app.kafka.topics.short-url-clicked}")
  private String topic;

  @Override
  public void publish(ShortUrlClickedEvent event) {
    kafkaTemplate.send(topic, event.eventId().toString(), event);
  }
}
