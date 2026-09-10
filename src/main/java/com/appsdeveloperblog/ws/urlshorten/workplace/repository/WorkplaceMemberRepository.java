package com.appsdeveloperblog.ws.urlshorten.workplace.repository;

import com.appsdeveloperblog.ws.urlshorten.workplace.model.entity.WorkplaceMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkplaceMemberRepository extends JpaRepository<WorkplaceMember, UUID> {

  Optional<WorkplaceMember> findByWorkplace_IdAndUser_Id(UUID workplaceId, UUID userId);

  List<WorkplaceMember> findAllByWorkplace_Id(UUID workplaceId);
}
