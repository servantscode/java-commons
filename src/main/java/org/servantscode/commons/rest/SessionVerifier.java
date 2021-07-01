package org.servantscode.commons.rest;

import com.auth0.jwt.interfaces.DecodedJWT;

public interface SessionVerifier {
    boolean verifySession(String callingIp, String token, DecodedJWT jwt); }