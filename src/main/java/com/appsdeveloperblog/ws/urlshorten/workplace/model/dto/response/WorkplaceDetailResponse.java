package com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkplaceDetailResponse {
  private UUID id;
  private String name;
  private String description;
  private String role;
  private String ownerEmail;
  private Integer membersCount;
  private Integer linksCount;
  private String createdAt;
  private List<WorkplaceMemberResponse> members;
  private List<com.appsdeveloperblog.ws.urlshorten.url.model.response.UrlResponse> links;
}
