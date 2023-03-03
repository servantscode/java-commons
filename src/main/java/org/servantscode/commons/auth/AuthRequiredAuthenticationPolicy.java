package org.servantscode.commons.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedList;
import java.util.List;

public class AuthRequiredAuthenticationPolicy implements AuthorizationPolicy {
    public void applyPolicy(ContainerRequestContext requestContext, DecodedJWT jwt) {
        UriInfo uri = requestContext.getUriInfo();
        String uriPath = uri.getPath();

        if (jwt == null)
            throw new NotAuthorizedException("Not Authorized");
    }
}
