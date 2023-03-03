package org.servantscode.commons.rest;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.servantscode.commons.Organization;
import org.servantscode.commons.auth.*;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.commons.security.PermissionManager;
import org.servantscode.commons.security.SCSecurityContext;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

@Provider
@Priority(2000)
public class AuthFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(AuthFilter.class);

    @Deprecated
    public static void registerOptionalTokenApi(String method, String path, boolean includeSubPaths) {
        if(AUTHORIZATION_POLICY instanceof PathAuthorizationPolicy)
            ((PathAuthorizationPolicy)AUTHORIZATION_POLICY).registerOptionalTokenApi(method, path, includeSubPaths);
    }

    @Deprecated
    public static void registerPublicApi(String method, String path, boolean includeSubPaths) {
        if(AUTHORIZATION_POLICY instanceof PathAuthorizationPolicy)
            ((PathAuthorizationPolicy)AUTHORIZATION_POLICY).registerPublicApi(method, path, includeSubPaths);
    }

    @Deprecated
    public static void registerPublicService(String path) {
        if(AUTHORIZATION_POLICY instanceof PathAuthorizationPolicy)
            ((PathAuthorizationPolicy)AUTHORIZATION_POLICY).registerPublicService(path);
    }

    private static SessionVerifier SESSION_VERIFIER = new DefaultSessionVerifier();
    private static TokenParser TOKEN_PARSER = new DefaultTokenParser();
    private static AuthorizationPolicy AUTHORIZATION_POLICY = new DefaultAuthorizationPolicy();

    public static void setSessionVerifier(SessionVerifier sessionVerifier) { SESSION_VERIFIER = sessionVerifier; }
    public static void setTokenParser(TokenParser tokenParser) { TOKEN_PARSER = tokenParser; }
    public static void setAuthorizationPolicy(AuthorizationPolicy authorizationPolicy) { AUTHORIZATION_POLICY = authorizationPolicy; }

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        //Allow OPTIONS calls for CORS
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS"))
            return;

        UriInfo uri = requestContext.getUriInfo();
        String uriPath = uri.getPath();
        if(uriPath.equals("ping"))
            return;

        String org = requestContext.getHeaderString("x-sc-org");
        if(isSet(org))
            LOG.trace("Found incoming sc-org header: " + org);
        if(isEmpty(org)) {
            String host = requestContext.getHeaderString("referer");
            host = isSet(host) ? URI.create(host).getHost() : requestContext.getHeaderString("x-forwarded-host");
            host = isSet(host) ? host : uri.getRequestUri().getHost();
            org = host.split("\\.")[0];
        }
        if(isSet(org)) {
            OrganizationContext.enableOrganization(org);
            ThreadContext.put("request.org", org);
        }

        ThreadContext.put("request.received", Long.toString(System.currentTimeMillis()));
        ThreadContext.put("request.method", requestContext.getMethod());
        ThreadContext.put("request.path", uriPath);

        String callingIp = request.getRemoteAddr();
        ThreadContext.put("request.origin", callingIp);

        String userAgent = requestContext.getHeaderString("user-agent");
        ThreadContext.put("request.user.agent", userAgent);

        ThreadContext.put("transaction.id", requestContext.getHeaderString("x-sc-transaction-id"));
        if (isEmpty(ThreadContext.get("transaction.id")))
            ThreadContext.put("transaction.id", UUID.randomUUID().toString());

        String token = retrieveTokenHeader(requestContext);
        DecodedJWT jwt = TOKEN_PARSER.parseToken(token);

        AUTHORIZATION_POLICY.applyPolicy(requestContext, jwt);

        if(jwt != null) {
            SESSION_VERIFIER.verifySession(callingIp, token, jwt);

            ThreadContext.put("user", jwt.getSubject());

            if (OrganizationContext.isMultiTenant()) {
                Organization activeOrg = OrganizationContext.getOrganization();
                if (activeOrg == null || !activeOrg.getName().equals(getOrg(jwt)))
                    throw new NotAuthorizedException("Not Authorized");
            }

            enableRole(jwt);
            requestContext.setSecurityContext(new SCSecurityContext(requestContext.getUriInfo(), jwt));
        }
    }

    // ----- Private -----
    private String retrieveTokenHeader(ContainerRequestContext requestContext) {
        List<String> authHeaders = requestContext.getHeaders().get("Authorization");
        if(authHeaders == null || authHeaders.size() != 1)
            return null;

        String authHeader = authHeaders.get(0);
        if(isEmpty(authHeader))
            return null;

        String[] headerBits = authHeader.split("\\s");
        if(headerBits.length != 2 || !headerBits[0].equalsIgnoreCase("Bearer"))
            return null;
        return headerBits[1];
    }

    private void enableRole(DecodedJWT jwt) {
        Claim claim = jwt.getClaim("permissions");
        if(claim != null) {
            String[] userPerms = claim.asArray(String.class);
            PermissionManager.enablePermissions(userPerms);
        }
    }

    private String getOrg(DecodedJWT jwt) {
        Claim claim = jwt.getClaim("org");
        if(claim == null)
            throw new NotAuthorizedException("Token does not have an organization");

        return claim.asString();
    }
}