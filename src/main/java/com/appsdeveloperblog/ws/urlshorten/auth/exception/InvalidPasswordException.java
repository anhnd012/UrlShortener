package com.appsdeveloperblog.ws.urlshorten.auth.exception;

import java.util.List;

public class InvalidPasswordException extends RuntimeException {
  private final List<String> violations;

  public InvalidPasswordException(List<String> violations) {
    super("Password does not satisfy the password policy");
    this.violations = List.copyOf(violations);
  }

  public List<String> getViolations() {
    return violations;
  }
}
