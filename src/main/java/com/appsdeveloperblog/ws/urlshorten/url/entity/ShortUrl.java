package com.appsdeveloperblog.ws.urlshorten.url.entity;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.common.entity.BaseEntity;
import com.appsdeveloperblog.ws.urlshorten.url.model.enums.UrlStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_short_url_short_code", columnNames = "short_code")
    })
public class ShortUrl extends BaseEntity {

  @Column(nullable = false, length = 8)
  private String shortCode;

  private String longUrl;

  @Enumerated(EnumType.STRING)
  private UrlStatus status;

  private Instant validFrom;

  private Instant expiresAt;

  private Long numberOfClicks;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "workplace_id")
  private com.appsdeveloperblog.ws.urlshorten.workplace.model.entity.Workplace workplace;
}
