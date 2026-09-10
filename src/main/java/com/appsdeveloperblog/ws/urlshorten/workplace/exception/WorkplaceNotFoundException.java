package com.appsdeveloperblog.ws.urlshorten.workplace.exception;

public class WorkplaceNotFoundException extends RuntimeException {
  public WorkplaceNotFoundException(String message) {
    super(message);
  }

  public WorkplaceNotFoundException() {
    super("Workplace not found.");
  }
}
