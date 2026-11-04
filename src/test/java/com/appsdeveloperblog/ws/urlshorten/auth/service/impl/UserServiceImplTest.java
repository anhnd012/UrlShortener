package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.EmailAlreadyExistsException;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordException;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.RoleNotFoundException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.CreateAdminRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.Role;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.RoleRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class UserServiceImplTest {
  private UserRepository users;
  private RoleRepository roles;
  private PasswordEncoder encoder;
  private UserServiceImpl service;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    roles = mock(RoleRepository.class);
    encoder = mock(PasswordEncoder.class);
    service = new UserServiceImpl(users, roles, encoder);
  }

  @Test
  void normalizesEmailHashesPasswordAndReusesThePersistedAdminRole() {
    Role admin = Role.builder().name("ADMIN").key("admin").build();
    UUID userId = UUID.randomUUID();
    when(roles.findByName("ADMIN")).thenReturn(Optional.of(admin));
    when(encoder.encode("calm river under a silver moon")).thenReturn("encoded-password");
    when(users.saveAndFlush(any(User.class)))
        .thenAnswer(
            invocation -> {
              User saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", userId);
              return saved;
            });

    var response =
        service.createAdmin(request(" Admin@Example.com ", "calm river under a silver moon"));

    var captured = ArgumentCaptor.forClass(User.class);
    verify(users).saveAndFlush(captured.capture());
    assertEquals("admin@example.com", captured.getValue().getEmail());
    assertEquals("encoded-password", captured.getValue().getPasswordHash());
    assertSame(admin, captured.getValue().getRoles().iterator().next());
    assertEquals(userId, response.id());
    assertEquals("admin@example.com", response.username());
    assertEquals(Set.of("ADMIN"), response.roles());
    verify(encoder).encode("calm river under a silver moon");
    verify(roles, never()).save(any());
  }

  @Test
  void rejectsAnExistingUserWithoutChangingTheirPasswordOrRoles() {
    when(users.findByEmailIgnoreCase("existing@example.com")).thenReturn(Optional.of(new User()));
    assertThrows(
        EmailAlreadyExistsException.class,
        () ->
            service.createAdmin(request("existing@example.com", "calm river under a silver moon")));
    verify(users, never()).saveAndFlush(any());
    verifyNoInteractions(roles, encoder);
  }

  @Test
  void rejectsInvalidPasswordBeforeHashingOrWriting() {
    assertThrows(
        InvalidPasswordException.class,
        () -> service.createAdmin(request("admin@example.com", "short")));
    verifyNoInteractions(users, roles, encoder);
  }

  @Test
  void missingAdminRoleDoesNotCreateAUser() {
    when(roles.findByName("ADMIN")).thenReturn(Optional.empty());
    assertThrows(
        RoleNotFoundException.class,
        () -> service.createAdmin(request("admin@example.com", "calm river under a silver moon")));
    verify(users, never()).saveAndFlush(any());
    verifyNoInteractions(encoder);
  }

  @Test
  void translatesAnEmailUniqueViolationEvenIfThePrecheckPassed() {
    when(roles.findByName("ADMIN")).thenReturn(Optional.of(Role.builder().name("ADMIN").build()));
    var cause =
        new ConstraintViolationException(
            "duplicate", new SQLException("duplicate", "23505"), "uq_app_user_email_normalized");
    when(users.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("duplicate", cause));
    assertThrows(
        EmailAlreadyExistsException.class,
        () -> service.createAdmin(request("admin@example.com", "calm river under a silver moon")));
  }

  @Test
  void doesNotMisreportOtherDatabaseFailuresAsDuplicateEmails() {
    when(roles.findByName("ADMIN")).thenReturn(Optional.of(Role.builder().name("ADMIN").build()));
    var failure = new DataIntegrityViolationException("different constraint");
    when(users.saveAndFlush(any())).thenThrow(failure);
    assertSame(
        failure,
        assertThrows(
            DataIntegrityViolationException.class,
            () ->
                service.createAdmin(
                    request("admin@example.com", "calm river under a silver moon"))));
  }

  private CreateAdminRequest request(String username, String password) {
    var request = new CreateAdminRequest();
    request.setEmail(username);
    request.setPassword(password);
    return request;
  }
}
