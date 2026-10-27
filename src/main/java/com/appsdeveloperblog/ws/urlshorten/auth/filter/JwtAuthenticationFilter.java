package com.appsdeveloperblog.ws.urlshorten.auth.filter;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.AuthenticatedUser;
import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.auth.repository.UserRepository;
import com.appsdeveloperblog.ws.urlshorten.auth.service.AccessTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final AccessTokenService accessTokenService;
  private final UserRepository userRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");

    if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {

      filterChain.doFilter(request, response);
      return;
    }

    try {
      String token = header.substring(7).trim();
      if (!StringUtils.hasText(token)) {
        throw new BadCredentialsException("Bearer token must not be empty.");
      }
      Claims claims = accessTokenService.verify(token);
      if (claims == null || !StringUtils.hasText(claims.getSubject())) {
        throw new BadCredentialsException("Access token has no subject.");
      }
      UUID userId = UUID.fromString(claims.getSubject());
      User user =
          userRepository
              .findWithRolesById(userId)
              .orElseThrow(() -> new BadCredentialsException("User no longer exists."));
      var principal =
          new AuthenticatedUser(user.getId(), user.getEmail(), null, user.getAuthorities());
      Authentication authentication =
          UsernamePasswordAuthenticationToken.authenticated(
              principal, null, principal.getAuthorities());
      var context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
    } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
      SecurityContextHolder.clearContext();
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    filterChain.doFilter(request, response);
  }
}
