package com.appsdeveloperblog.ws.urlshorten.workplace.model.entity;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.common.entity.BaseEntity;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.enums.WorkplaceRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "workplace_member",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_workplace_member",
          columnNames = {"workplace_id", "user_id"})
    })
public class WorkplaceMember extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workplace_id", nullable = false)
  private Workplace workplace;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  private WorkplaceRole role;
}
