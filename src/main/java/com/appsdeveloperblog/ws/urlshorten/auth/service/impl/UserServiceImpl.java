package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.constant.RoleEnum;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.EmailAlreadyExistsException;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.RoleNotFoundException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.CreateAdminRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response.UserResponse;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.Role;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.RoleRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.UserService;
import com.appsdeveloperblog.ws.urlshorten.auth.validation.PasswordPolicy;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public UserResponse createAdmin(CreateAdminRequest request) {
    PasswordPolicy.validate(request.getPassword());
    String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
    if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
      throw new EmailAlreadyExistsException();
    }

    Role adminRole =
        roleRepository
            .findByName(RoleEnum.ADMIN.getName())
            .orElseThrow(() -> new RoleNotFoundException(RoleEnum.ADMIN.getName()));
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.getRoles().add(adminRole);

    User saved;
    try {
      saved = userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException exception) {
      if (isEmailUniqueViolation(exception)) {
        throw new EmailAlreadyExistsException();
      }
      throw exception;
    }
    return new UserResponse(saved.getId(), saved.getEmail(), Set.of(adminRole.getName()));
  }

  private boolean isEmailUniqueViolation(DataIntegrityViolationException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof org.hibernate.exception.ConstraintViolationException violation) {
        return "uq_app_user_email_normalized".equals(violation.getConstraintName());
      }
      cause = cause.getCause();
    }
    return false;
  }

  @Override
  public List<UserResponse> getUsers(Integer page, Integer size) {
    //    Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

    Pageable pageable = PageRequest.of(page, size);

    return userRepository
        .findAll(pageable)
        .map(
            user ->
                new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet())))
        .toList();
  }
}
