package com.appsdeveloperblog.ws.urlshorten.auth.model.entity;

import com.appsdeveloperblog.ws.urlshorten.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "app_user")
public class User extends BaseEntity {

  @Column(nullable = false, length = 320)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(length = 100)
  private String name;

  @Column(name = "image_url", length = 2048)
  private String imageUrl;
}
