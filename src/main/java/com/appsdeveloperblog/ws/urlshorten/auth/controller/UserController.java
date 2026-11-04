package com.appsdeveloperblog.ws.urlshorten.auth.controller;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response.UserResponse;
import com.appsdeveloperblog.ws.urlshorten.auth.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping()
  @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
  public ResponseEntity<List<UserResponse>> getUsers(
      @RequestParam Integer page, @RequestParam Integer size) {
    List<UserResponse> users = userService.getUsers(page, size);
    return ResponseEntity.status(HttpStatus.OK).body(users);
  }
}
