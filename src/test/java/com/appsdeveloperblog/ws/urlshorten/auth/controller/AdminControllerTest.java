package com.appsdeveloperblog.ws.urlshorten.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.appsdeveloperblog.ws.urlshorten.auth.exception.EmailAlreadyExistsException;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.InvalidPasswordException;
import com.appsdeveloperblog.ws.urlshorten.auth.exception.RoleNotFoundException;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response.UserResponse;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.Role;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.AccessTokenService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.UserService;
import com.appsdeveloperblog.ws.urlshorten.common.exception.GlobalExceptionHandler;
import com.appsdeveloperblog.ws.urlshorten.config.SecurityConfig;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = AdminController.class,
    properties = {
      "spring.config.name=admin-api-test",
      "logging.level.org.springframework.security=WARN"
    })
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ImportAutoConfiguration({
  SecurityFilterAutoConfiguration.class,
  ServletWebSecurityAutoConfiguration.class
})
class AdminControllerTest {
  private static final String REQUEST =
      """
      {"username":"admin@example.com","password":"calm river under a silver moon"}
      """;

  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService userDetailsService;
  @MockitoBean private UserService userService;
  @MockitoBean private UserRepository users;
  @MockitoBean private AccessTokenService tokens;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void superAdminCanCreateAdminWithoutPasswordFieldsInResponse() throws Exception {
    authenticate("SUPER_ADMIN");
    UUID id = UUID.randomUUID();
    when(userService.createAdmin(any()))
        .thenReturn(new UserResponse(id, "admin@example.com", Set.of("ADMIN")));

    mvc.perform(
            post("/admins")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.username").value("admin@example.com"))
        .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
    verify(userService).createAdmin(any());
  }

  @Test
  void missingTokenIsUnauthorizedBeforeCallingTheService() throws Exception {
    mvc.perform(post("/admins").contentType(MediaType.APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(userService);
  }

  @Test
  void adminIsForbidden() throws Exception {
    authenticate("ADMIN");
    assertForbidden();
  }

  @Test
  void regularUserIsForbidden() throws Exception {
    authenticate("USER");
    assertForbidden();
  }

  @Test
  void authenticatedUserWithNoRolesIsForbidden() throws Exception {
    authenticate();
    assertForbidden();
  }

  @Test
  void rolesAreTakenFromDatabaseNotFromTokenClaims() throws Exception {
    UUID id = authenticate("USER");
    when(tokens.verify("valid-token"))
        .thenReturn(
            Jwts.claims().subject(id.toString()).add("roles", List.of("SUPER_ADMIN")).build());
    assertForbidden();
  }

  @Test
  void malformedTokenIsUnauthorized() throws Exception {
    when(tokens.verify("invalid")).thenThrow(new MalformedJwtException("Invalid test token"));
    assertUnauthorized("Bearer invalid");
  }

  @Test
  void expiredTokenIsUnauthorized() throws Exception {
    when(tokens.verify("expired"))
        .thenThrow(new ExpiredJwtException(null, null, "Expired test token"));
    assertUnauthorized("Bearer expired");
  }

  @Test
  void emptyBearerTokenIsUnauthorized() throws Exception {
    assertUnauthorized("Bearer ");
    verifyNoInteractions(tokens);
  }

  @Test
  void invalidSubjectIsUnauthorized() throws Exception {
    when(tokens.verify("invalid-subject")).thenReturn(Jwts.claims().subject("not-a-uuid").build());
    assertUnauthorized("Bearer invalid-subject");
  }

  @Test
  void deletedUserIsUnauthorized() throws Exception {
    when(tokens.verify("deleted-user"))
        .thenReturn(Jwts.claims().subject(UUID.randomUUID().toString()).build());
    assertUnauthorized("Bearer deleted-user");
  }

  @Test
  void invalidRequestIsBadRequest() throws Exception {
    authenticate("SUPER_ADMIN");
    mvc.perform(
            post("/admins")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"not-an-email\",\"password\":\"test\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(userService);
  }

  @Test
  void passwordPolicyFailureIsBadRequest() throws Exception {
    authenticate("SUPER_ADMIN");
    when(userService.createAdmin(any()))
        .thenThrow(new InvalidPasswordException(List.of("Password is too short.")));
    mvc.perform(
            post("/admins")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isBadRequest());
  }

  @Test
  void existingEmailIsConflict() throws Exception {
    authenticate("SUPER_ADMIN");
    when(userService.createAdmin(any())).thenThrow(new EmailAlreadyExistsException());
    mvc.perform(
            post("/admins")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isConflict());
  }

  @Test
  void missingSeededRoleIsServerError() throws Exception {
    authenticate("SUPER_ADMIN");
    when(userService.createAdmin(any())).thenThrow(new RoleNotFoundException("ADMIN"));
    mvc.perform(
            post("/admins")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void loginPrincipalAlsoReceivesDatabaseAuthorities() {
    User user = new User();
    user.setEmail("superadmin@example.com");
    user.getRoles().add(Role.builder().name("SUPER_ADMIN").build());
    when(users.findWithRolesByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
    var details = userDetailsService.loadUserByUsername(user.getEmail());
    assertEquals(
        List.of("ROLE_SUPER_ADMIN"),
        details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
  }

  private UUID authenticate(String... roleNames) {
    UUID id = UUID.randomUUID();
    User user = new User();
    ReflectionTestUtils.setField(user, "id", id);
    user.setEmail("caller@example.com");
    for (String name : roleNames) {
      user.getRoles().add(Role.builder().name(name).build());
    }
    when(tokens.verify("valid-token")).thenReturn(Jwts.claims().subject(id.toString()).build());
    when(users.findWithRolesById(id)).thenReturn(Optional.of(user));
    return id;
  }

  private void assertForbidden() throws Exception {
    mvc.perform(
            post("/admins")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isForbidden());
    verifyNoInteractions(userService);
  }

  private void assertUnauthorized(String authorization) throws Exception {
    mvc.perform(
            post("/admins")
                .header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(userService);
  }
}
