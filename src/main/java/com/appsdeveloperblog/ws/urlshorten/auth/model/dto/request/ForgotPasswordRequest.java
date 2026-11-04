package com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

  @NotBlank(message = "Email is required.")
  @Email(message = "Invalid email format.")
  @Size(max = 254, message = "Email must not exceed 254 characters.")
  private String email;
}
