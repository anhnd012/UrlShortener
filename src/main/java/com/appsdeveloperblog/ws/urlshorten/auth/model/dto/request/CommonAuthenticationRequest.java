package com.appsdeveloperblog.ws.urlshorten.auth.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public abstract class CommonAuthenticationRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
