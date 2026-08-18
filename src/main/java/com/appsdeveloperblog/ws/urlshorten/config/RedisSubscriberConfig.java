package com.appsdeveloperblog.ws.urlshorten.config;

import com.appsdeveloperblog.ws.urlshorten.url.messaging.RedisClickSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisSubscriberConfig {

  @Bean
  RedisMessageListenerContainer redisContainer(
      RedisConnectionFactory connectionFactory,
      RedisClickSubscriber subscriber,
      @Value("${app.redis.click-channel}") String channel) {

    RedisMessageListenerContainer container = new RedisMessageListenerContainer();

    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(subscriber, new ChannelTopic(channel));

    return container;
  }
}
