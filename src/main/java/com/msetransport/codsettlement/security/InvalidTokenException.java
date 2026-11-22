package com.msetransport.codsettlement.security;

import org.springframework.security.core.AuthenticationException;

/** Expired, tampered, malformed or wrong-type token. Always results in 401. */
public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
