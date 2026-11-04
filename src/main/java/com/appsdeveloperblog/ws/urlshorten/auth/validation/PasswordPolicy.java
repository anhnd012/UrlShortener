package com.appsdeveloperblog.ws.urlshorten.auth.validation;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PasswordPolicy {

  private static final int MIN_PASSWORD_LENGTH = 12;
  private static final int MAX_PASSWORD_LENGTH = 128;

  // This is an MVP denylist. Replace or extend it with a maintained breach corpus later.
  private static final Set<String> KNOWN_COMPROMISED_PASSWORDS =
      Set.of(
          "123456",
          "123456789",
          "1234567890",
          "password",
          "password1",
          "password123",
          "qwerty",
          "qwerty123",
          "admin",
          "admin123",
          "welcome",
          "welcome1",
          "letmein",
          "changeme",
          "iloveyou",
          "abc123",
          "monkey",
          "dragon",
          "sunshine",
          "princess",
          "football",
          "correcthorsebatterystaple");

  private PasswordPolicy() {}

  public static void validate(String password) {
    List<String> violations = new ArrayList<>();

    if (password == null || password.isBlank()) {
      violations.add("Password must not be blank.");
    } else {
      int passwordLength = password.codePointCount(0, password.length());
      if (passwordLength < MIN_PASSWORD_LENGTH) {
        violations.add("Password must contain at least 12 characters.");
      }
      if (passwordLength > MAX_PASSWORD_LENGTH) {
        violations.add("Password must not contain more than 128 characters.");
      }
      if (KNOWN_COMPROMISED_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
        violations.add("Password is too common or has appeared in a known compromised list.");
      }
      if (isRepeatedSingleCharacter(password)) {
        violations.add("Password must not consist of one repeated character.");
      }
    }

    if (!violations.isEmpty()) {
      throw new InvalidPasswordException(violations);
    }
  }

  private static boolean isRepeatedSingleCharacter(String password) {
    int[] codePoints = password.codePoints().toArray();
    if (codePoints.length < 3) {
      return false;
    }

    for (int index = 1; index < codePoints.length; index++) {
      if (codePoints[index] != codePoints[0]) {
        return false;
      }
    }
    return true;
  }
}
