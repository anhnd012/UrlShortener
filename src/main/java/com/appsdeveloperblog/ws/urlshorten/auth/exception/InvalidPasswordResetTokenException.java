package com.appsdeveloperblog.ws.urlshorten.auth.exception;

public class InvalidPasswordResetTokenException extends RuntimeException {

  public InvalidPasswordResetTokenException() {
    super("The password reset link is invalid or has expired.");
  }
}
