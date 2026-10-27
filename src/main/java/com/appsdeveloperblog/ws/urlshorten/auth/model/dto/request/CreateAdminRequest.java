package com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAdminRequest {
  @NotBlank
  @Email
  @Size(max = 320)
  private String email;

  @NotBlank private String password;

  public void setEmail(String email) {
    this.email = email == null ? null : email.trim();
  }
}
