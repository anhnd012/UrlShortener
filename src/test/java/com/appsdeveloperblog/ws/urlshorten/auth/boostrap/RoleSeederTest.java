package com.appsdeveloperblog.ws.urlshorten.auth.boostrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.Role;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.RoleRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RoleSeederTest {
  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void createsAllRolesWhenDatabaseIsEmpty() {
    var repository = mock(RoleRepository.class);
    when(repository.findByNames(anyList())).thenReturn(List.of());
    new RoleSeeder(repository).loadRoles();
    ArgumentCaptor<List<Role>> captured = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captured.capture());
    assertEquals(3, captured.getValue().size());
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void comparesRoleNamesAndOnlyCreatesMissingRoles() {
    var repository = mock(RoleRepository.class);
    when(repository.findByNames(anyList()))
        .thenReturn(List.of(Role.builder().name("USER").key("user").build()));
    new RoleSeeder(repository).loadRoles();
    ArgumentCaptor<List<Role>> captured = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captured.capture());
    assertEquals(
        Set.of("ADMIN", "SUPER_ADMIN"),
        captured.getValue().stream().map(Role::getName).collect(Collectors.toSet()));
  }

  @Test
  void doesNotInsertAgainWhenEveryRoleExists() {
    var repository = mock(RoleRepository.class);
    when(repository.findByNames(anyList()))
        .thenReturn(
            List.of(
                Role.builder().name("USER").build(),
                Role.builder().name("ADMIN").build(),
                Role.builder().name("SUPER_ADMIN").build()));
    new RoleSeeder(repository).loadRoles();
    verify(repository, never()).saveAll(any());
  }
}
