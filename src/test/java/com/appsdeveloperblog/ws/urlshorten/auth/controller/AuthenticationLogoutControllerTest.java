package com.appsdeveloperblog.ws.urlshorten.auth.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.appsdeveloperblog.ws.urlshorten.auth.service.AuthenticationService;
import com.appsdeveloperblog.ws.urlshorten.auth.service.PasswordResetService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuthenticationController.class)
@Import(AuthenticationLogoutControllerTest.TestSecurityConfig.class)
@ImportAutoConfiguration({SecurityFilterAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
class AuthenticationLogoutControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthenticationService authenticationService;

  @MockitoBean private PasswordResetService passwordResetService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void logoutInvalidatesSessionAndClearsSessionCookie() throws Exception {
    MockHttpSession session = new MockHttpSession();
    TestingAuthenticationToken authentication =
        new TestingAuthenticationToken("user@example.com", null, "ROLE_USER");
    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
    securityContext.setAuthentication(authentication);
    session.setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

    MvcResult result =
        mockMvc
            .perform(post("/auth/logout").session(session))
            .andExpect(status().isNoContent())
            .andReturn();

    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);

    assertTrue(session.isInvalid());
    assertTrue(setCookie != null && setCookie.contains("SESSION="));
    assertTrue(setCookie != null && setCookie.contains("Max-Age=0"));
  }

  @Test
  void logoutWithoutSessionIsStillIdempotent() throws Exception {
    mockMvc.perform(post("/auth/logout")).andExpect(status().isNoContent());
  }

  @TestConfiguration
  static class TestSecurityConfig {

    @Bean
    SecurityFilterChain logoutSecurityFilterChain(HttpSecurity http) throws Exception {
      http
          .csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
          .logout(
              logout ->
                  logout
                      .logoutUrl("/auth/logout")
                      .invalidateHttpSession(true)
                      .clearAuthentication(true)
                      .deleteCookies("SESSION")
                      .permitAll()
                      .logoutSuccessHandler(
                          (request, response, authentication) ->
                              response.setStatus(HttpServletResponse.SC_NO_CONTENT)));

      return http.build();
    }
  }
}
