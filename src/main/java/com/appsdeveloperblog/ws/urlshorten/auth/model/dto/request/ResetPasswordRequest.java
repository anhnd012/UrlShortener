package com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

  @NotBlank(message = "Reset token is required.")
  @Size(max = 256, message = "Reset token is invalid.")
  private String token;

  @NotBlank(message = "New password is required.")
  private String newPassword;

  @NotBlank(message = "Password confirmation is required.")
  private String confirmPassword;
}
