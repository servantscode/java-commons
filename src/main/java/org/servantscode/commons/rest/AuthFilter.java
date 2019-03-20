package org.servantscode.commons.rest;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.servantscode.commons.EnvProperty;
import org.servantscode.commons.security.PermissionManager;
import org.servantscode.commons.security.SCSecurityContext;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.UUID;

import static org.servantscode.commons.StringUtils.isEmpty;

@Provider
@Priority(2000)
public class AuthFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(AuthFilter.class);

    private static final String SIGNING_KEY = EnvProperty.get("JWT_KEY");

    private static final Algorithm algorithm = Algorithm.HMAC256(SIGNING_KEY);
    private static final JWTVerifier VERIFIER = JWT.require(algorithm)
            .acceptLeeway(1)   //1 sec leeway for date checks to account for clock slop
            .withIssuer("Servant's Code")
            .build();

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        //Allow OPTIONS calls for CORS
        if(requestContext.getMethod().equalsIgnoreCase("OPTIONS"))
            return;

        ThreadContext.put("request.received", Long.toString(System.currentTimeMillis()));
        ThreadContext.put("request.method", requestContext.getMethod());
        ThreadContext.put("request.path", requestContext.getUriInfo().getPath());
        ThreadContext.put("request.orgin", request.getRemoteAddr());

        if(isEmpty(ThreadContext.get("transaction.id")))
            ThreadContext.put("transaction.id", UUID.randomUUID().toString());

        // No token required for login.
        // TODO: Is there a better way to do this with routing?
        if(requestContext.getUriInfo().getPath().equalsIgnoreCase("login") &&
                requestContext.getMethod().equalsIgnoreCase("POST"))
            return;

        try {
            String token = parseAuthHeader(requestContext);
            DecodedJWT jwt = VERIFIER.verify(token);
            enableRole(jwt);
            SecurityContext context = createContext(requestContext.getUriInfo(), jwt);
            requestContext.setSecurityContext(context);
            ThreadContext.put("user", context.getUserPrincipal().getName());
        } catch (JWTVerificationException e) {
            LOG.warn("Invalid jwt token presented.", e);
            throw new NotAuthorizedException("Not Authorized");
        }
    }

    // ----- Private -----
    private String parseAuthHeader(ContainerRequestContext requestContext) {
        List<String> authHeaders = requestContext.getHeaders().get("Authorization");
        if(authHeaders == null || authHeaders.size() != 1)
            throw new NotAuthorizedException("Not Authorized");

        String authHeader = authHeaders.get(0);
        if(isEmpty(authHeader))
            throw new NotAuthorizedException("Not Authorized");

        String[] headerBits = authHeader.split("\\s");
        if(headerBits.length != 2 || !headerBits[0].equalsIgnoreCase("Bearer"))
            throw new NotAuthorizedException("Not Authorized");
        return headerBits[1];
    }

    private SecurityContext createContext(UriInfo uriInfo, DecodedJWT jwt) {
        return new SCSecurityContext(uriInfo, jwt);
    }

    private void enableRole(DecodedJWT jwt) {
        Claim claim = jwt.getClaim("permissions");
        if(claim != null) {
            String[] userPerms = claim.asArray(String.class);
            PermissionManager.enablePermissions(userPerms);
        }
    }
}