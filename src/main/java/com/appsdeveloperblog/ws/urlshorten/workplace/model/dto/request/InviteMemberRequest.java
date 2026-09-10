package com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteMemberRequest {

  @NotBlank(message = "Email must not be blank.")
  @Email(message = "Invalid email format.")
  private String email;
}
