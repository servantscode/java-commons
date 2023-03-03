package org.servantscode.commons.auth;

import com.auth0.jwt.interfaces.DecodedJWT;

import javax.ws.rs.container.ContainerRequestContext;

public interface AuthorizationPolicy {
    public void applyPolicy (ContainerRequestContext context, DecodedJWT jwt);
}
