package com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {

  @NotBlank(message = "Profile name must not be blank.")
  @Size(max = 100, message = "Profile name must not exceed 100 characters.")
  private String name;

  private String imageUrl;
}
