package com.appsdeveloperblog.ws.urlshorten.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

class WebConfigTest {

  @Test
  void addCorsMappingsRegistersApplicationCorsPolicy() {
    WebConfig webConfig = new WebConfig();
    CorsRegistry registry = new CorsRegistry();

    assertDoesNotThrow(() -> webConfig.addCorsMappings(registry));
  }
}
