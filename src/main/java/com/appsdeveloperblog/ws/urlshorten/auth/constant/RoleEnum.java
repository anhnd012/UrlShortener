package com.appsdeveloperblog.ws.urlshorten.auth.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleEnum {
  ADMIN("ADMIN", "admin"),
  USER("USER", "user"),
  SUPER_ADMIN("SUPER_ADMIN", "super_admin");

  private String name;
  private String key;
}
