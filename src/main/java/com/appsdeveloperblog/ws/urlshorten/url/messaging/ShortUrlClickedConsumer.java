package com.appsdeveloperblog.ws.urlshorten.url.messaging;

import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;
import com.appsdeveloperblog.ws.urlshorten.url.exception.InvalidUrlException;
import com.appsdeveloperblog.ws.urlshorten.url.exception.ShortUrlNotFoundException;
import com.appsdeveloperblog.ws.urlshorten.url.repository.ProcessedClickEventRepository;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShortUrlClickedConsumer {
    private final UrlRepository urlRepository;
    private final ProcessedClickEventRepository processedClickEventRepository;

    @KafkaListener(topics = "${app.kafka.topics.short-url-clicked}")
    @Transactional
    public void consume(ShortUrlClickedEvent event) {
        if(event.version() != 1 || event.shortCode() == null) {
            throw new InvalidUrlException("Invalid event");
        }
        int affectedRow = processedClickEventRepository.increasingClickEvents(event.eventId(), event.shortCode(), event.occurredAt());
        if(affectedRow == 0) {
            log.info("[ShortUrlClickedConsumer - consume()] eventId={} has existed ", event.eventId());
            return;
        }
        int updatedRows = urlRepository.increasingShortUrlClicks(event.shortCode());
        if(updatedRows != 1) {
            throw new ShortUrlNotFoundException(event.shortCode());
        }
    }

}
