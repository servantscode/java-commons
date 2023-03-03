package org.servantscode.commons.auth;

import com.auth0.jwt.interfaces.DecodedJWT;

public interface TokenParser {
    public DecodedJWT parseToken(String token);
}
