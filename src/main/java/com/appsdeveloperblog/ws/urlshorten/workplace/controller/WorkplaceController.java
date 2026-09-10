package com.appsdeveloperblog.ws.urlshorten.workplace.controller;

import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.request.CreateWorkplaceRequest;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.request.InviteMemberRequest;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response.WorkplaceDetailResponse;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response.WorkplaceMemberResponse;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response.WorkplaceResponse;
import com.appsdeveloperblog.ws.urlshorten.workplace.service.WorkplaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping({"/workplaces", "/api/v1/workplaces"})
public class WorkplaceController {

  private final WorkplaceService workplaceService;

  @GetMapping
  public ResponseEntity<List<WorkplaceResponse>> getWorkplaces() {
    List<WorkplaceResponse> responses = workplaceService.getWorkplaces();
    return ResponseEntity.ok(responses);
  }

  @GetMapping("/{id}")
  public ResponseEntity<WorkplaceDetailResponse> getWorkplace(@PathVariable UUID id) {
    WorkplaceDetailResponse response = workplaceService.getWorkplace(id);
    return ResponseEntity.ok(response);
  }

  @PostMapping
  public ResponseEntity<WorkplaceResponse> createWorkplace(
      @RequestBody @Valid CreateWorkplaceRequest request) {
    WorkplaceResponse response = workplaceService.createWorkplace(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping({"/{id}/invites", "/{id}/members"})
  public ResponseEntity<WorkplaceMemberResponse> inviteMember(
      @PathVariable UUID id,
      @RequestBody @Valid InviteMemberRequest request) {
    WorkplaceMemberResponse response = workplaceService.inviteMember(id, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
