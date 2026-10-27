package com.appsdeveloperblog.ws.urlshorten.auth.exception;

public class RoleNotFoundException extends RuntimeException {
  public RoleNotFoundException(String roleName) {
    super("Required role " + roleName + " has not been seeded.");
  }
}
