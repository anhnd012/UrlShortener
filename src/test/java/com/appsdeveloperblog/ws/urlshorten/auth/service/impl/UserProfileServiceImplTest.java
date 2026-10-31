package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.AuthenticatedUser;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.UpdateUserProfileRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response.UserProfileResponse;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.storage.service.FileStorageService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class UserProfileServiceImplTest {

  private UserRepository userRepository;
  private FileStorageService fileStorageService;
  private UserProfileServiceImpl userProfileService;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    userRepository = mock(UserRepository.class);
    fileStorageService = mock(FileStorageService.class);
    userProfileService = new UserProfileServiceImpl(userRepository, fileStorageService);
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentProfileWhenNameIsNullDefaultsToEmailPrefix() {
    User user = new User();
    user.setEmail("anhnd0123@gmail.com");
    user.setName(null);
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

    authenticate(user);

    UserProfileResponse response = userProfileService.getCurrentProfile();

    assertAll(
        () -> assertNotNull(response),
        () -> assertEquals("anhnd0123@gmail.com", response.getEmail()),
        () -> assertEquals("anhnd0123", response.getDisplayName()));
    verify(userRepository).findByEmail(user.getEmail());
    verify(userRepository, never()).findAll();
  }

  @Test
  void getCurrentProfileWhenNameIsSetReturnsCustomName() {
    User user = new User();
    user.setEmail("anhnd0123@gmail.com");
    user.setName("Duc Anh Nguyen");
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

    authenticate(user);

    UserProfileResponse response = userProfileService.getCurrentProfile();

    assertAll(
        () -> assertNotNull(response),
        () -> assertEquals("anhnd0123@gmail.com", response.getEmail()),
        () -> assertEquals("Duc Anh Nguyen", response.getName()),
        () -> assertEquals("Duc Anh Nguyen", response.getDisplayName()));
  }

  @Test
  void updateProfileUpdatesNameAndReturnsUpdatedProfile() {
    User user = new User();
    user.setEmail("anhnd0123@gmail.com");
    user.setName(null);
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

    authenticate(user);
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UpdateUserProfileRequest request =
        UpdateUserProfileRequest.builder().name("Duc Anh Nguyen").build();

    UserProfileResponse response = userProfileService.updateProfile(request);

    assertAll(
        () -> assertNotNull(response),
        () -> assertEquals("Duc Anh Nguyen", response.getName()),
        () -> assertEquals("Duc Anh Nguyen", response.getDisplayName()),
        () -> assertEquals("anhnd0123@gmail.com", response.getEmail()));

    verify(userRepository).saveAndFlush(any(User.class));
  }

  @Test
  void uploadAvatarSuccessfullyUpdatesUserImageUrl() {
    User user = new User();
    user.setEmail("anhnd0123@gmail.com");
    user.setName("Duc Anh");
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

    MockMultipartFile mockFile =
        new MockMultipartFile("file", "avatar.png", "image/png", "test-image-data".getBytes());

    authenticate(user);
    when(fileStorageService.uploadFile(mockFile, "avatars"))
        .thenReturn("http://localhost:9000/url-shortener-avatars/avatars/avatar-uuid.png");
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserProfileResponse response = userProfileService.uploadAvatar(mockFile);

    assertAll(
        () -> assertNotNull(response),
        () ->
            assertEquals(
                "http://localhost:9000/url-shortener-avatars/avatars/avatar-uuid.png",
                response.getImageUrl()),
        () -> assertEquals("Duc Anh", response.getName()));

    verify(fileStorageService).uploadFile(mockFile, "avatars");
    verify(userRepository).saveAndFlush(any(User.class));
  }

  @Test
  void removeAvatarDeletesExistingFileAndResetsImageUrl() {
    User user = new User();
    user.setEmail("anhnd0123@gmail.com");
    user.setImageUrl("http://localhost:9000/url-shortener-avatars/avatars/old-avatar.png");
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());

    authenticate(user);
    when(userRepository.saveAndFlush(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserProfileResponse response = userProfileService.removeAvatar();

    assertAll(() -> assertNotNull(response), () -> assertEquals(null, response.getImageUrl()));

    verify(fileStorageService)
        .deleteFile("http://localhost:9000/url-shortener-avatars/avatars/old-avatar.png");
    verify(userRepository).saveAndFlush(any(User.class));
  }

  @Test
  void getCurrentUserWithoutAuthenticationDoesNotQueryDatabase() {
    assertNull(userProfileService.getCurrentUser());
    verifyNoInteractions(userRepository, fileStorageService);
  }

  private void authenticate(User user) {
    var principal = new AuthenticatedUser(user.getId(), user.getEmail(), null, List.of());
    var authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
  }
}
