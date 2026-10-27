package com.appsdeveloperblog.ws.urlshorten.auth.boostrap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.Role;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.RoleRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class SuperAdminSeederTest {
  private UserRepository users;
  private RoleRepository roles;
  private PasswordEncoder encoder;
  private SuperAdminSeeder seeder;

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    roles = mock(RoleRepository.class);
    encoder = mock(PasswordEncoder.class);
    seeder = new SuperAdminSeeder(users, encoder, roles);
    ReflectionTestUtils.setField(seeder, "superAdminEmail", " SuperAdmin@Example.com ");
    ReflectionTestUtils.setField(seeder, "superAdminPassword", "test-only-bootstrap-password");
  }

  @Test
  void skipsExistingAccountBeforeReadingRoleOrHashingPassword() {
    when(users.findByEmailIgnoreCase("superadmin@example.com")).thenReturn(Optional.of(new User()));
    ReflectionTestUtils.setField(seeder, "superAdminPassword", "");
    seeder.createSuperAdmin();
    verify(users, never()).save(any());
    verifyNoInteractions(roles, encoder);
  }

  @Test
  void createsAccountWithExistingRoleAndEncodedPassword() {
    Role superAdmin = Role.builder().name("SUPER_ADMIN").build();
    when(roles.findByName("SUPER_ADMIN")).thenReturn(Optional.of(superAdmin));
    when(encoder.encode("test-only-bootstrap-password")).thenReturn("encoded-password");
    seeder.createSuperAdmin();
    var captured = ArgumentCaptor.forClass(User.class);
    verify(users).save(captured.capture());
    assertEquals("superadmin@example.com", captured.getValue().getEmail());
    assertEquals("encoded-password", captured.getValue().getPasswordHash());
    assertSame(superAdmin, captured.getValue().getRoles().iterator().next());
  }

  @Test
  void missingRoleFailsClearlyWithoutSavingAccount() {
    when(roles.findByName("SUPER_ADMIN")).thenReturn(Optional.empty());
    var failure = assertThrows(IllegalStateException.class, seeder::createSuperAdmin);
    assertTrue(failure.getMessage().contains("SUPER_ADMIN role"));
    verify(users, never()).save(any());
    verifyNoInteractions(encoder);
  }

  @Test
  void missingPasswordForNewAccountFailsWithoutSaving() {
    ReflectionTestUtils.setField(seeder, "superAdminPassword", "");
    assertThrows(IllegalStateException.class, seeder::createSuperAdmin);
    verify(users, never()).save(any());
    verifyNoInteractions(roles, encoder);
  }
}
