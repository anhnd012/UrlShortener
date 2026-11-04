package com.appsdeveloperblog.ws.urlshorten.auth.boostrap;

import com.appsdeveloperblog.ws.urlshorten.auth.constant.RoleEnum;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.Role;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.RoleRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
@RequiredArgsConstructor
public class RoleSeeder implements ApplicationListener<ContextRefreshedEvent> {
  private final RoleRepository roleRepository;

  @Override
  @Transactional
  public void onApplicationEvent(ContextRefreshedEvent event) {
    loadRoles();
  }

  public void loadRoles() {
    List<Role> roles =
        Arrays.asList(
            Role.builder().name(RoleEnum.USER.getName()).key(RoleEnum.USER.getKey()).build(),
            Role.builder().name(RoleEnum.ADMIN.getName()).key(RoleEnum.ADMIN.getKey()).build(),
            Role.builder()
                .name(RoleEnum.SUPER_ADMIN.getName())
                .key(RoleEnum.SUPER_ADMIN.getKey())
                .build());

    List<String> roleNames = roles.stream().map(role -> role.getName()).toList();
    List<Role> savedRoles = roleRepository.findByNames(roleNames);
    Set<String> savedRoleNames = savedRoles.stream().map(Role::getName).collect(Collectors.toSet());
    List<Role> unsavedRoles =
        roles.stream()
            .filter(role -> !savedRoleNames.contains(role.getName()))
            .collect(Collectors.toList());

    if (!unsavedRoles.isEmpty()) {
      roleRepository.saveAll(unsavedRoles);
    }
  }
}
