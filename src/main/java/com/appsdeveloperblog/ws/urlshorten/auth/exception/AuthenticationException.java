package com.appsdeveloperblog.ws.urlshorten.auth.exception;

public class AuthenticationException extends RuntimeException{

    public AuthenticationException() {
        super("Email or password are not correct");
    }
}
