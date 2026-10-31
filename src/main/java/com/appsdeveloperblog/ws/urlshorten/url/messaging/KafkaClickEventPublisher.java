package com.appsdeveloperblog.ws.urlshorten.url.messaging;

import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaClickEventPublisher implements ClickEventPublisher {
  private final KafkaTemplate<String, ShortUrlClickedEvent> kafkaTemplate;

  @Value("${app.kafka.topics.short-url-clicked}")
  private String topic;

  @Override
  public void publish(ShortUrlClickedEvent event) {
    CompletableFuture<SendResult<String, ShortUrlClickedEvent>> future =
        kafkaTemplate.send(topic, event.eventId().toString(), event);

    future
        .thenAccept(
            result -> {
              var metadata = result.getRecordMetadata();

              log.info(
                  "Kafka ACK: topic={}, partition={}, offset={}",
                  metadata.topic(),
                  metadata.partition(),
                  metadata.offset());
            })
        .exceptionally(
            error -> {
              log.error("Kafka send failed", error);
              return null;
            });
  }
}
