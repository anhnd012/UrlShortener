package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.UpdateUserProfileRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response.UserProfileResponse;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.UserProfileService;
import com.appsdeveloperblog.ws.urlshorten.storage.service.FileStorageService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

  private final UserRepository userRepository;
  private final FileStorageService fileStorageService;

  @Override
  @Transactional(readOnly = true)
  public UserProfileResponse getCurrentProfile() {
    User currentUser = getCurrentUser();
    return mapToProfileResponse(currentUser);
  }

  @Override
  @Transactional
  public UserProfileResponse updateProfile(UpdateUserProfileRequest request) {
    User currentUser = getCurrentUser();

    currentUser.setName(request.getName().trim());
    if (StringUtils.hasText(request.getImageUrl())) {
      currentUser.setImageUrl(request.getImageUrl());
    }

    User saved = userRepository.saveAndFlush(currentUser);
    return mapToProfileResponse(saved);
  }

  @Override
  @Transactional
  public UserProfileResponse uploadAvatar(MultipartFile file) {
    User currentUser = getCurrentUser();

    if (StringUtils.hasText(currentUser.getImageUrl())) {
      fileStorageService.deleteFile(currentUser.getImageUrl());
    }

    String avatarUrl = fileStorageService.uploadFile(file, "avatars");
    currentUser.setImageUrl(avatarUrl);

    User saved = userRepository.saveAndFlush(currentUser);
    return mapToProfileResponse(saved);
  }

  @Override
  @Transactional
  public UserProfileResponse removeAvatar() {
    User currentUser = getCurrentUser();

    if (StringUtils.hasText(currentUser.getImageUrl())) {
      fileStorageService.deleteFile(currentUser.getImageUrl());
      currentUser.setImageUrl(null);
    }

    User saved = userRepository.saveAndFlush(currentUser);
    return mapToProfileResponse(saved);
  }

  private UserProfileResponse mapToProfileResponse(User user) {
    String email = user.getEmail() != null ? user.getEmail() : "user@example.com";
    String displayName = deriveDisplayName(user.getName(), email);

    return UserProfileResponse.builder()
        .id(user.getId())
        .email(email)
        .name(user.getName())
        .displayName(displayName)
        .imageUrl(user.getImageUrl())
        .createdAt(
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : Instant.now().toString())
        .build();
  }

  private String deriveDisplayName(String name, String email) {
    if (StringUtils.hasText(name)) {
      return name.trim();
    }

    if (StringUtils.hasText(email)) {
      int atIndex = email.indexOf('@');
      if (atIndex > 0) {
        return email.substring(0, atIndex);
      }
      return email;
    }

    return "User";
  }

  @Override
  public User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }

    String email = authentication.getName();
    User currentUser = userRepository.findByEmail(email).orElse(null);

    return currentUser;
  }
}
