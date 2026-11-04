package com.appsdeveloperblog.ws.urlshorten.auth.controller;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.UpdateUserProfileRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response.UserProfileResponse;
import com.appsdeveloperblog.ws.urlshorten.auth.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping({"/api/v1/user/profile", "/api/v1/users/profile", "/api/v1/users/me"})
public class UserProfileController {

  private final UserProfileService userProfileService;

  @GetMapping
  public ResponseEntity<UserProfileResponse> getProfile() {
    UserProfileResponse response = userProfileService.getCurrentProfile();
    return ResponseEntity.ok(response);
  }

  @PutMapping
  public ResponseEntity<UserProfileResponse> updateProfile(
      @RequestBody @Valid UpdateUserProfileRequest request) {
    UserProfileResponse response = userProfileService.updateProfile(request);
    return ResponseEntity.ok(response);
  }

  @PatchMapping
  public ResponseEntity<UserProfileResponse> patchProfile(
      @RequestBody @Valid UpdateUserProfileRequest request) {
    UserProfileResponse response = userProfileService.updateProfile(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<UserProfileResponse> uploadAvatar(
      @RequestParam("file") MultipartFile file) {
    UserProfileResponse response = userProfileService.uploadAvatar(file);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/avatar")
  public ResponseEntity<UserProfileResponse> removeAvatar() {
    UserProfileResponse response = userProfileService.removeAvatar();
    return ResponseEntity.ok(response);
  }
}
