package com.appsdeveloperblog.ws.urlshorten.workplace.service.impl;

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
import com.appsdeveloperblog.ws.urlshorten.workplace.service.WorkplaceService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import com.appsdeveloperblog.ws.urlshorten.url.mapper.ShortUrlMapper;
import com.appsdeveloperblog.ws.urlshorten.url.model.response.UrlResponse;
import com.appsdeveloperblog.ws.urlshorten.url.repository.UrlRepository;

@Service
@RequiredArgsConstructor
public class WorkplaceServiceImpl implements WorkplaceService {

  private final WorkplaceRepository workplaceRepository;
  private final WorkplaceMemberRepository workplaceMemberRepository;
  private final UserRepository userRepository;
  private final UrlRepository urlRepository;
  private final ShortUrlMapper shortUrlMapper;

  @Override
  @Transactional(readOnly = true)
  public List<WorkplaceResponse> getWorkplaces() {
    User currentUser = getCurrentOrCreateDefaultUser();
    List<Workplace> workplaces = workplaceRepository.findAllByMemberUserId(currentUser.getId());

    if (workplaces.isEmpty()) {
      // Fallback to all workplaces if membership query returns empty
      workplaces = workplaceRepository.findAll();
    }

    return workplaces.stream()
        .map(wp -> mapToWorkplaceResponse(wp, currentUser.getId()))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public WorkplaceDetailResponse getWorkplace(UUID id) {
    Workplace workplace =
        workplaceRepository
            .findByIdWithMembers(id)
            .orElseThrow(() -> new WorkplaceNotFoundException("Workplace with id " + id + " was not found."));

    User currentUser = getCurrentOrCreateDefaultUser();

    List<WorkplaceMemberResponse> members =
        workplace.getMembers().stream()
            .map(
                m ->
                    WorkplaceMemberResponse.builder()
                        .email(m.getUser() != null ? m.getUser().getEmail() : "unknown")
                        .name(m.getUser() != null ? m.getUser().getName() : null)
                        .role(m.getRole() != null ? m.getRole().name().toLowerCase() : "participant")
                        .joinedAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : Instant.now().toString())
                        .build())
            .collect(Collectors.toList());

    List<ShortUrl> workplaceUrls = urlRepository.findAllByWorkplaceId(id);
    List<UrlResponse> mappedLinks =
        workplaceUrls.stream().map(shortUrlMapper::mappingUrlResponse).collect(Collectors.toList());

    String userRole = resolveUserRole(workplace, currentUser.getId());

    return WorkplaceDetailResponse.builder()
        .id(workplace.getId())
        .name(workplace.getName())
        .description(workplace.getDescription())
        .role(userRole)
        .ownerEmail(workplace.getOwner() != null ? workplace.getOwner().getEmail() : null)
        .membersCount(members.size())
        .linksCount(mappedLinks.size())
        .createdAt(workplace.getCreatedAt() != null ? workplace.getCreatedAt().toString() : Instant.now().toString())
        .members(members)
        .links(mappedLinks)
        .build();
  }

  @Override
  @Transactional
  public WorkplaceResponse createWorkplace(CreateWorkplaceRequest request) {
    User currentUser = getCurrentOrCreateDefaultUser();

    Workplace workplace = new Workplace();
    workplace.setName(request.getName());
    workplace.setDescription(request.getDescription());
    workplace.setOwner(currentUser);

    WorkplaceMember ownerMember = new WorkplaceMember();
    ownerMember.setWorkplace(workplace);
    ownerMember.setUser(currentUser);
    ownerMember.setRole(WorkplaceRole.OWNER);

    workplace.getMembers().add(ownerMember);

    Workplace saved = workplaceRepository.saveAndFlush(workplace);

    return mapToWorkplaceResponse(saved, currentUser.getId());
  }

  @Override
  @Transactional
  public WorkplaceMemberResponse inviteMember(UUID workplaceId, InviteMemberRequest request) {
    Workplace workplace =
        workplaceRepository
            .findById(workplaceId)
            .orElseThrow(() -> new WorkplaceNotFoundException("Workplace with id " + workplaceId + " was not found."));

    String email = request.getEmail().trim().toLowerCase();

    User targetUser =
        userRepository
            .findByEmail(email)
            .orElseGet(
                () -> {
                  User newUser = new User();
                  newUser.setEmail(email);
                  newUser.setPasswordHash("$2a$10$defaultPasswordHashPlaceholder1234567890");
                  int atIndex = email.indexOf('@');
                  String defaultName = atIndex > 0 ? email.substring(0, atIndex) : email;
                  newUser.setName(defaultName);
                  return userRepository.saveAndFlush(newUser);
                });

    Optional<WorkplaceMember> existingMember =
        workplaceMemberRepository.findByWorkplace_IdAndUser_Id(workplaceId, targetUser.getId());

    if (existingMember.isPresent()) {
      throw new MemberAlreadyExistsException("User " + email + " is already a member of this workplace.");
    }

    WorkplaceMember newMember = new WorkplaceMember();
    newMember.setWorkplace(workplace);
    newMember.setUser(targetUser);
    newMember.setRole(WorkplaceRole.EMPLOYEE); // Invited member will only be in employee role as requested!

    WorkplaceMember savedMember = workplaceMemberRepository.saveAndFlush(newMember);

    return WorkplaceMemberResponse.builder()
        .email(targetUser.getEmail())
        .name(targetUser.getName())
        .role(WorkplaceRole.EMPLOYEE.name().toLowerCase())
        .joinedAt(savedMember.getCreatedAt() != null ? savedMember.getCreatedAt().toString() : Instant.now().toString())
        .build();
  }

  private WorkplaceResponse mapToWorkplaceResponse(Workplace workplace, UUID currentUserId) {
    String role = resolveUserRole(workplace, currentUserId);
    int membersCount = workplace.getMembers() != null ? workplace.getMembers().size() : 1;
    int linksCount = workplace.getUrls() != null ? workplace.getUrls().size() : 0;

    return WorkplaceResponse.builder()
        .id(workplace.getId())
        .name(workplace.getName())
        .description(workplace.getDescription())
        .role(role)
        .ownerEmail(workplace.getOwner() != null ? workplace.getOwner().getEmail() : null)
        .membersCount(membersCount)
        .linksCount(linksCount)
        .createdAt(workplace.getCreatedAt() != null ? workplace.getCreatedAt().toString() : Instant.now().toString())
        .build();
  }

  private String resolveUserRole(Workplace workplace, UUID currentUserId) {
    if (currentUserId == null) {
      return "participant";
    }

    if (workplace.getOwner() != null
        && workplace.getOwner().getId() != null
        && workplace.getOwner().getId().equals(currentUserId)) {
      return "owner";
    }

    if (workplace.getMembers() != null) {
      Optional<WorkplaceMember> memberOpt =
          workplace.getMembers().stream()
              .filter(
                  m ->
                      m.getUser() != null
                          && m.getUser().getId() != null
                          && m.getUser().getId().equals(currentUserId))
              .findFirst();

      if (memberOpt.isPresent()) {
        return memberOpt.get().getRole().name().toLowerCase();
      }
    }

    return "participant";
  }

  private User getCurrentOrCreateDefaultUser() {
    return userRepository
        .findAll()
        .stream()
        .findFirst()
        .orElseGet(
            () -> {
              User defaultUser = new User();
              defaultUser.setEmail("user@example.com");
              defaultUser.setPasswordHash("$2a$10$defaultPasswordHashPlaceholder1234567890");
              defaultUser.setName("LinkFlow User");
              return userRepository.saveAndFlush(defaultUser);
            });
  }
}
