package com.appsdeveloperblog.ws.urlshorten.auth.service;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.UpdateUserProfileRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response.UserProfileResponse;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

  UserProfileResponse getCurrentProfile();

  UserProfileResponse updateProfile(UpdateUserProfileRequest request);

  UserProfileResponse uploadAvatar(MultipartFile file);

  UserProfileResponse removeAvatar();

  User getCurrentUser();
}
