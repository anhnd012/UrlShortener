package com.appsdeveloperblog.ws.urlshorten.workplace.repository;

import com.appsdeveloperblog.ws.urlshorten.workplace.model.entity.Workplace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkplaceRepository extends JpaRepository<Workplace, UUID> {

  @Query(
      "SELECT DISTINCT w FROM Workplace w JOIN w.members m WHERE m.user.id = :userId ORDER BY w.createdAt DESC")
  List<Workplace> findAllByMemberUserId(@Param("userId") UUID userId);

  @Query(
      "SELECT w FROM Workplace w LEFT JOIN FETCH w.members m LEFT JOIN FETCH m.user WHERE w.id = :id")
  Optional<Workplace> findByIdWithMembers(@Param("id") UUID id);
}
