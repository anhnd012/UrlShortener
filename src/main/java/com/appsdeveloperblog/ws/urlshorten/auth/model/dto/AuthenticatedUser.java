package com.appsdeveloperblog.ws.urlshorten.auth.model.dto;

import java.util.Collection;
import java.util.UUID;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
public class AuthenticatedUser implements UserDetails {
  private final UUID userId;
  private final String email;
  private final String password;
  private final Collection<? extends GrantedAuthority> authorities;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getUsername() {
    return email;
  }
}
