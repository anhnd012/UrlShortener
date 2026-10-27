package com.appsdeveloperblog.ws.urlshorten.config;

import com.appsdeveloperblog.ws.urlshorten.auth.filter.JwtAuthenticationFilter;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.AuthenticatedUser;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.AccessTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final AccessTokenService accessTokenService;
  private final UserRepository userRepository;

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository repository)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .addFilterBefore(
            new JwtAuthenticationFilter(accessTokenService, userRepository),
            UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/admins")
                    .hasRole("SUPER_ADMIN")
                    .requestMatchers("/urls/**", "/api/v1/workplaces/**", "/workplaces/**", "/users/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .securityContext(context -> context.securityContextRepository(repository))
        .logout(
            logout ->
                logout
                    .logoutUrl("/auth/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("SESSION")
                    .deleteCookies("refresh_token")
                    .permitAll()
                    .logoutSuccessHandler(
                        (request, response, authentication) ->
                            response.setStatus(HttpServletResponse.SC_NO_CONTENT)));

    return http.build();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  public SecurityContextRepository securityContextRepository() {
    return new NullSecurityContextRepository();
  }

  @Bean
  public UserDetailsService userDetailsService(UserRepository userRepository) {
    return email ->
        userRepository
            .findWithRolesByEmailIgnoreCase(email)
            .map(
                user ->
                    new AuthenticatedUser(
                        user.getId(),
                        user.getEmail(),
                        user.getPasswordHash(),
                        user.getAuthorities()))
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  @Bean
  public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }
}
