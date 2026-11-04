package com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
  private UUID id;
  private String email;
  private String name;
  private String displayName;
  private String imageUrl;
  private String createdAt;
}
