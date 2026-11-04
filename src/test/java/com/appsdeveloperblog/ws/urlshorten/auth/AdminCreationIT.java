package com.appsdeveloperblog.ws.urlshorten.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.appsdeveloperblog.ws.urlshorten.auth.boostrap.RoleSeeder;
import com.appsdeveloperblog.ws.urlshorten.auth.boostrap.SuperAdminSeeder;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.EmailAlreadyExistsException;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.RoleNotFoundException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.CreateAdminRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.RoleRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.UserService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.impl.UserServiceImpl;
import com.appsdeveloperblog.ws.urlshorten.config.FlywayConfig;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest(
    showSql = false,
    properties = {
      "spring.config.name=admin-api-test",
      "spring.jpa.hibernate.ddl-auto=validate",
      "SUPER_ADMIN_EMAIL=bootstrap@example.test",
      "SUPER_ADMIN_PASSWORD=test-only-bootstrap-password"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  FlywayConfig.class,
  UserServiceImpl.class,
  RoleSeeder.class,
  SuperAdminSeeder.class,
  AdminCreationIT.EncoderConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers
class AdminCreationIT {
  @Container @ServiceConnection
  static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

  @Autowired private UserService service;
  @Autowired private UserRepository users;
  @Autowired private RoleRepository roles;
  @Autowired private RoleSeeder roleSeeder;
  @Autowired private SuperAdminSeeder superAdminSeeder;
  @Autowired private ApplicationContext context;
  @Autowired private PasswordEncoder encoder;
  @Autowired private JdbcTemplate jdbc;

  @TestConfiguration
  static class EncoderConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
      return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
  }

  @BeforeEach
  void resetUsersAndEnsureRolesExist() {
    jdbc.update("DELETE FROM user_role");
    users.deleteAllInBatch();
    roleSeeder.onApplicationEvent(new ContextRefreshedEvent(context));
  }

  @Test
  void persistsRealHashAndLinksAnExistingRoleWithoutLazyLoadingErrors() {
    var existingRole = roles.findByName("ADMIN").orElseThrow();
    var response = service.createAdmin(request(" ADMIN@Example.com "));
    var saved = users.findWithRolesByEmailIgnoreCase("ADMIN@EXAMPLE.COM").orElseThrow();
    assertEquals(response.id(), saved.getId());
    assertEquals("admin@example.com", saved.getEmail());
    assertNotEquals("calm river under a silver moon", saved.getPasswordHash());
    assertTrue(encoder.matches("calm river under a silver moon", saved.getPasswordHash()));
    assertEquals(Set.of("ADMIN"), response.roles());
    assertEquals(existingRole.getId(), saved.getRoles().iterator().next().getId());
    assertEquals("ROLE_ADMIN", saved.getAuthorities().iterator().next().getAuthority());
    assertEquals(3, roles.count());
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_role", Integer.class));
  }

  @Test
  void rejectsExistingAccountCaseInsensitivelyWithoutResettingPassword() {
    service.createAdmin(request("admin@example.com"));
    String originalHash =
        users.findByEmailIgnoreCase("admin@example.com").orElseThrow().getPasswordHash();
    assertThrows(
        EmailAlreadyExistsException.class, () -> service.createAdmin(request("ADMIN@EXAMPLE.COM")));
    assertEquals(1, users.count());
    assertEquals(
        originalHash,
        users.findByEmailIgnoreCase("admin@example.com").orElseThrow().getPasswordHash());
  }

  @Test
  void missingRoleDoesNotLeaveAPartiallyCreatedUser() {
    roles.delete(roles.findByName("ADMIN").orElseThrow());
    assertThrows(
        RoleNotFoundException.class, () -> service.createAdmin(request("admin@example.com")));
    assertEquals(0, users.count());
    assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM user_role", Integer.class));
  }

  @Test
  void repeatedSeedingPreservesTheExistingAccountAndPassword() {
    var event = new ContextRefreshedEvent(context);
    superAdminSeeder.onApplicationEvent(event);
    var original = users.findByEmailIgnoreCase("bootstrap@example.test").orElseThrow();
    roleSeeder.onApplicationEvent(event);
    superAdminSeeder.onApplicationEvent(event);
    var afterRestart = users.findWithRolesByEmailIgnoreCase("bootstrap@example.test").orElseThrow();
    assertEquals(1, users.count());
    assertEquals(3, roles.count());
    assertEquals(original.getId(), afterRestart.getId());
    assertEquals(original.getPasswordHash(), afterRestart.getPasswordHash());
    assertEquals("SUPER_ADMIN", afterRestart.getRoles().iterator().next().getName());
  }

  @Test
  void concurrentRequestsProduceOneUserAndOneConflict() throws Exception {
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> createAfterSignal(start, "admin@example.com"));
      var second = executor.submit(() -> createAfterSignal(start, "ADMIN@EXAMPLE.COM"));
      start.countDown();
      var statuses =
          List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)).stream()
              .sorted()
              .toList();
      assertEquals(List.of(201, 409), statuses);
    }
    assertEquals(1, users.count());
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM user_role", Integer.class));
  }

  private int createAfterSignal(CountDownLatch start, String email) throws InterruptedException {
    if (!start.await(5, TimeUnit.SECONDS)) {
      throw new IllegalStateException("Concurrent test did not start.");
    }
    try {
      service.createAdmin(request(email));
      return 201;
    } catch (EmailAlreadyExistsException exception) {
      return 409;
    }
  }

  private CreateAdminRequest request(String username) {
    var request = new CreateAdminRequest();
    request.setEmail(username);
    request.setPassword("calm river under a silver moon");
    return request;
  }
}
