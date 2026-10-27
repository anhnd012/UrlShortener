package com.appsdeveloperblog.ws.urlshorten.auth.repository;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  @EntityGraph(attributePaths = "roles")
  Optional<User> findWithRolesById(UUID id);

  @EntityGraph(attributePaths = "roles")
  Optional<User> findWithRolesByEmailIgnoreCase(String email);

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmail(String email);
}
