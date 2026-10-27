package com.appsdeveloperblog.ws.urlshorten.auth.boostrap;

import com.appsdeveloperblog.ws.urlshorten.auth.constant.RoleEnum;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.Role;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.RoleRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Order(2)
@RequiredArgsConstructor
public class SuperAdminSeeder implements ApplicationListener<ContextRefreshedEvent> {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;

  @Value("${SUPER_ADMIN_EMAIL}")
  private String superAdminEmail;

  @Value("${SUPER_ADMIN_PASSWORD:}")
  private String superAdminPassword;

  @Override
  @Transactional
  public void onApplicationEvent(ContextRefreshedEvent event) {
    createSuperAdmin();
  }

  public void createSuperAdmin() {
    if (!StringUtils.hasText(superAdminEmail)) {
      throw new IllegalStateException("SUPER_ADMIN_EMAIL must be configured.");
    }
    String email = superAdminEmail.trim().toLowerCase(Locale.ROOT);
    if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
      return;
    }
    if (!StringUtils.hasText(superAdminPassword)) {
      throw new IllegalStateException(
          "SUPER_ADMIN_PASSWORD must be configured to create the initial super admin.");
    }

    Role admin =
        roleRepository
            .findByName(RoleEnum.SUPER_ADMIN.getName())
            .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role has not been seeded."));
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(superAdminPassword));
    user.getRoles().add(admin);
    userRepository.save(user);
  }
}
