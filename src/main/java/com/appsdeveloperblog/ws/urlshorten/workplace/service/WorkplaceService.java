package com.appsdeveloperblog.ws.urlshorten.workplace.service;

import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.request.CreateWorkplaceRequest;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.request.InviteMemberRequest;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response.WorkplaceDetailResponse;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response.WorkplaceMemberResponse;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response.WorkplaceResponse;
import java.util.List;
import java.util.UUID;

public interface WorkplaceService {

  List<WorkplaceResponse> getWorkplaces();

  WorkplaceDetailResponse getWorkplace(UUID id);

  WorkplaceResponse createWorkplace(CreateWorkplaceRequest request);

  WorkplaceMemberResponse inviteMember(UUID workplaceId, InviteMemberRequest request);
}
