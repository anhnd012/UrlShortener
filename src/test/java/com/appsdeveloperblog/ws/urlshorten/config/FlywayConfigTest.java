package com.appsdeveloperblog.ws.urlshorten.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

class FlywayConfigTest {

  @Test
  void flywayMigratorRunsMigration() {
    Flyway flyway = mock(Flyway.class);

    Object migrator = new FlywayConfig().flywayMigrator(flyway);

    verify(flyway).migrate();
    assertNotNull(migrator);
  }

  @Test
  void entityManagerFactoryPostProcessorDoesNothingWhenBeanIsAbsent() {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    FlywayConfig.entityManagerFactoryDependsOnFlyway().postProcessBeanFactory(beanFactory);

    assertFalse(beanFactory.containsBeanDefinition("entityManagerFactory"));
  }

  @Test
  void entityManagerFactoryPostProcessorAddsFlywayDependencyAndKeepsExistingDependencies() {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Object.class);
    beanDefinition.setDependsOn("existingDependency", "flywayMigrator");
    beanFactory.registerBeanDefinition("entityManagerFactory", beanDefinition);
    BeanFactoryPostProcessor postProcessor = FlywayConfig.entityManagerFactoryDependsOnFlyway();

    postProcessor.postProcessBeanFactory(beanFactory);

    assertArrayEquals(
        new String[] {"existingDependency", "flywayMigrator"}, beanDefinition.getDependsOn());
  }

  @Test
  void entityManagerFactoryPostProcessorHandlesMissingDependencies() {
    DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Object.class);
    beanFactory.registerBeanDefinition("entityManagerFactory", beanDefinition);

    FlywayConfig.entityManagerFactoryDependsOnFlyway().postProcessBeanFactory(beanFactory);

    assertArrayEquals(new String[] {"flywayMigrator"}, beanDefinition.getDependsOn());
  }
}
