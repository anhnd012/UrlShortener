package com.appsdeveloperblog.ws.urlshorten.auth.controller;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.CreateAdminRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response.UserResponse;
import com.appsdeveloperblog.ws.urlshorten.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admins")
@RequiredArgsConstructor
public class AdminController {
  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.createAdmin(request));
  }
}
