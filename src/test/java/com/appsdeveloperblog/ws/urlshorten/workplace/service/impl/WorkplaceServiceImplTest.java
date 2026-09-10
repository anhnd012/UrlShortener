package com.appsdeveloperblog.ws.urlshorten.workplace.service.impl;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.workplace.exception.MemberAlreadyExistsException;
import com.appsdeveloperblog.ws.urlshorten.workplace.exception.WorkplaceNotFoundException;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.request.CreateWorkplaceRequest;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.request.InviteMemberRequest;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response.WorkplaceDetailResponse;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response.WorkplaceMemberResponse;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.dto.response.WorkplaceResponse;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.entity.Workplace;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.entity.WorkplaceMember;
import com.appsdeveloperblog.ws.urlshorten.workplace.model.enums.WorkplaceRole;
import com.appsdeveloperblog.ws.urlshorten.workplace.repository.WorkplaceMemberRepository;
import com.appsdeveloperblog.ws.urlshorten.workplace.repository.WorkplaceRepository;
import com.appsdeveloperblog.ws.urlshorten.url.mapper.ShortUrlMapper;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WorkplaceServiceImplTest {

  private WorkplaceRepository workplaceRepository;
  private WorkplaceMemberRepository workplaceMemberRepository;
  private UserRepository userRepository;
  private UrlRepository urlRepository;
  private ShortUrlMapper shortUrlMapper;
  private WorkplaceServiceImpl workplaceService;

  @BeforeEach
  void setUp() {
    workplaceRepository = mock(WorkplaceRepository.class);
    workplaceMemberRepository = mock(WorkplaceMemberRepository.class);
    userRepository = mock(UserRepository.class);
    urlRepository = mock(UrlRepository.class);
    shortUrlMapper = mock(ShortUrlMapper.class);
    workplaceService =
        new WorkplaceServiceImpl(
            workplaceRepository, workplaceMemberRepository, userRepository, urlRepository, shortUrlMapper);
  }

  @Test
  void createWorkplaceSuccessfully() {
    UUID userId = UUID.randomUUID();
    User user = new User();
    user.setEmail("owner@example.com");
    ReflectionTestUtils.setField(user, "id", userId);

    when(userRepository.findAll()).thenReturn(List.of(user));
    when(workplaceRepository.saveAndFlush(any(Workplace.class)))
        .thenAnswer(
            invocation -> {
              Workplace saved = invocation.getArgument(0);
              ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
              return saved;
            });

    CreateWorkplaceRequest request =
        CreateWorkplaceRequest.builder()
            .name("Engineering Team")
            .description("Workspace for engineering")
            .build();

    WorkplaceResponse response = workplaceService.createWorkplace(request);

    assertAll(
        () -> assertNotNull(response),
        () -> assertEquals("Engineering Team", response.getName()),
        () -> assertEquals("Workspace for engineering", response.getDescription()),
        () -> assertEquals("owner", response.getRole()));
  }

  @Test
  void getWorkplaceThrowsNotFoundWhenNotExisting() {
    UUID randomId = UUID.randomUUID();
    when(workplaceRepository.findByIdWithMembers(randomId)).thenReturn(Optional.empty());

    assertThrows(
        WorkplaceNotFoundException.class, () -> workplaceService.getWorkplace(randomId));
  }

  @Test
  void getWorkplaceReturnsDetailsSuccessfully() {
    UUID workplaceId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    User owner = new User();
    owner.setEmail("owner@example.com");
    ReflectionTestUtils.setField(owner, "id", userId);

    Workplace workplace = new Workplace();
    ReflectionTestUtils.setField(workplace, "id", workplaceId);
    workplace.setName("Acme Corp");
    workplace.setDescription("Acme Corp main workspace");
    workplace.setOwner(owner);

    WorkplaceMember member = new WorkplaceMember();
    member.setWorkplace(workplace);
    member.setUser(owner);
    member.setRole(WorkplaceRole.OWNER);

    workplace.setMembers(List.of(member));

    when(userRepository.findAll()).thenReturn(List.of(owner));
    when(workplaceRepository.findByIdWithMembers(workplaceId)).thenReturn(Optional.of(workplace));

    WorkplaceDetailResponse response = workplaceService.getWorkplace(workplaceId);

    assertAll(
        () -> assertNotNull(response),
        () -> assertEquals("Acme Corp", response.getName()),
        () -> assertEquals("owner", response.getRole()),
        () -> assertEquals(1, response.getMembers().size()),
        () -> assertEquals("owner@example.com", response.getMembers().get(0).getEmail()));
  }

  @Test
  void inviteMemberSuccessfullyAsEmployeeRole() {
    UUID workplaceId = UUID.randomUUID();
    Workplace workplace = new Workplace();
    ReflectionTestUtils.setField(workplace, "id", workplaceId);

    User invitedUser = new User();
    invitedUser.setEmail("teammate@example.com");
    invitedUser.setName("Teammate");
    ReflectionTestUtils.setField(invitedUser, "id", UUID.randomUUID());

    when(workplaceRepository.findById(workplaceId)).thenReturn(Optional.of(workplace));
    when(userRepository.findByEmail("teammate@example.com")).thenReturn(Optional.of(invitedUser));
    when(workplaceMemberRepository.findByWorkplace_IdAndUser_Id(workplaceId, invitedUser.getId()))
        .thenReturn(Optional.empty());
    when(workplaceMemberRepository.saveAndFlush(any(WorkplaceMember.class)))
        .thenAnswer(
            invocation -> {
              WorkplaceMember m = invocation.getArgument(0);
              ReflectionTestUtils.setField(m, "id", UUID.randomUUID());
              return m;
            });

    InviteMemberRequest request = InviteMemberRequest.builder().email("teammate@example.com").build();
    WorkplaceMemberResponse response = workplaceService.inviteMember(workplaceId, request);

    assertAll(
        () -> assertNotNull(response),
        () -> assertEquals("teammate@example.com", response.getEmail()),
        () -> assertEquals("employee", response.getRole()));
  }

  @Test
  void inviteMemberThrowsExceptionWhenAlreadyMember() {
    UUID workplaceId = UUID.randomUUID();
    Workplace workplace = new Workplace();
    ReflectionTestUtils.setField(workplace, "id", workplaceId);

    User existingUser = new User();
    existingUser.setEmail("existing@example.com");
    ReflectionTestUtils.setField(existingUser, "id", UUID.randomUUID());

    WorkplaceMember member = new WorkplaceMember();
    member.setWorkplace(workplace);
    member.setUser(existingUser);
    member.setRole(WorkplaceRole.EMPLOYEE);

    when(workplaceRepository.findById(workplaceId)).thenReturn(Optional.of(workplace));
    when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));
    when(workplaceMemberRepository.findByWorkplace_IdAndUser_Id(workplaceId, existingUser.getId()))
        .thenReturn(Optional.of(member));

    InviteMemberRequest request = InviteMemberRequest.builder().email("existing@example.com").build();

    assertThrows(
        MemberAlreadyExistsException.class,
        () -> workplaceService.inviteMember(workplaceId, request));
  }
}
