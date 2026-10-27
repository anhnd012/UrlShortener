package com.appsdeveloperblog.ws.urlshorten.auth.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignInResponse {
    private String accessToken;
    private String refreshToken;
}
