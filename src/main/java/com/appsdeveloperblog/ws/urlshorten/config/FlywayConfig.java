package com.appsdeveloperblog.ws.urlshorten.config;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

  private static final String FLYWAY_MIGRATOR_BEAN_NAME = "flywayMigrator";

  @Bean
  public Flyway flyway(DataSource dataSource) {
    return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
  }

  @Bean(name = FLYWAY_MIGRATOR_BEAN_NAME)
  public Object flywayMigrator(Flyway flyway) {
    flyway.migrate();
    return new Object();
  }

  @Bean
  public static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
    return new BeanFactoryPostProcessor() {
      @Override
      public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
          throws BeansException {
        if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
          return;
        }

        BeanDefinition beanDefinition = beanFactory.getBeanDefinition("entityManagerFactory");
        String[] dependsOn = beanDefinition.getDependsOn();
        String[] updatedDependsOn =
            Stream.concat(
                    Arrays.stream(Objects.requireNonNullElse(dependsOn, new String[0])),
                    Stream.of(FLYWAY_MIGRATOR_BEAN_NAME))
                .distinct()
                .toArray(String[]::new);

        beanDefinition.setDependsOn(updatedDependsOn);
      }
    };
  }
}
