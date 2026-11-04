package com.appsdeveloperblog.ws.urlshorten.auth.repository;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, UUID> {

  @Query("SELECT r FROM Role r WHERE r.name IN (:names)")
  List<Role> findByNames(@Param("names") List<String> names);

  Optional<Role> findByName(String name);
}
