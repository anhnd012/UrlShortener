package com.appsdeveloperblog.ws.urlshorten.auth.service;

import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request.CreateAdminRequest;
import com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response.UserResponse;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface UserService {
  UserResponse createAdmin(CreateAdminRequest request);

  List<UserResponse> getUsers(Integer page, Integer size);
}
