package com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkplaceMemberResponse {
  private String email;
  private String role;
  private String joinedAt;
  private String name;
}
