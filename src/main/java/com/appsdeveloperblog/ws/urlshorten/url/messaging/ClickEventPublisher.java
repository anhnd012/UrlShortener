package com.appsdeveloperblog.ws.urlshorten.url.messaging;

import com.appsdeveloperblog.ws.urlshorten.url.event.ShortUrlClickedEvent;

public interface ClickEventPublisher {
  void publish(ShortUrlClickedEvent event);
}
