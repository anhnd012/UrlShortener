package com.appsdeveloperblog.ws.urlshorten.workplace.exception;

public class MemberAlreadyExistsException extends RuntimeException {
  public MemberAlreadyExistsException(String message) {
    super(message);
  }

  public MemberAlreadyExistsException() {
    super("User is already a member of this workplace.");
  }
}
