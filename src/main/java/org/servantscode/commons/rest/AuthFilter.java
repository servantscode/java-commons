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
import org.servantscode.commons.Organization;
import org.servantscode.commons.security.OrganizationContext;
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
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

@Provider
@Priority(2000)
public class AuthFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(AuthFilter.class);

    private static final String SIGNING_KEY = EnvProperty.get("JWT_KEY");
    private static final String JWT_ISSUER = EnvProperty.get("JWT_ISSUER", "Servant's Code");

    //Make sure these lists are lower cased for case-insensitive comparisons
    private static final List<RequestType> OPTIONAL_TOKEN_PATHS = new LinkedList<>();

    private static final List<RequestType> OPEN_PATHS = new LinkedList<>();
    public static final String ANY = "*";

    static {
        OPTIONAL_TOKEN_PATHS.addAll(asList(new RequestType("password"),
                new RequestType("GET", "photo/public/", true)));

        OPEN_PATHS.addAll(asList(new RequestType("login"),
                new RequestType("password/reset"),
                new RequestType("POST", "registration"),
                new RequestType("GET", "ping"),
                new RequestType("GET", "preference"),
                new RequestType("GET", "relationship/types"),
                new RequestType("GET", "calendar/public"),
                new RequestType("GET", "organization/active"),
                new RequestType("GET", "parish", true),
                new RequestType("GET", "pushpay", true)));
    }

    public static void registerOptionalTokenApi(String method, String path, boolean includeSubPaths) {
        OPTIONAL_TOKEN_PATHS.add(new RequestType(method.toUpperCase(), path.toLowerCase(), includeSubPaths));
        LOG.debug("Added optional token path: " + path);
    }

    public static void registerPublicApi(String method, String path, boolean includeSubPaths) {
        OPEN_PATHS.add(new RequestType(method.toUpperCase(), path.toLowerCase(), includeSubPaths));
        LOG.debug("Added open path: " + path);
    }

    public static void registerPublicService(String path) {
        registerPublicApi("*", path, true);
    }

    private static final Algorithm algorithm = Algorithm.HMAC256(SIGNING_KEY);
    private static final JWTVerifier VERIFIER = JWT.require(algorithm)
            .acceptLeeway(1)   //1 sec leeway for date checks to account for clock slop
            .withIssuer(JWT_ISSUER)
            .build();

    private static SessionVerifier SESSION_VERIFIER = new DefaultSessionVerifier();

    public static void setSessionVerifier(SessionVerifier sessionVerifier) {
        SESSION_VERIFIER = sessionVerifier;
    }

    @Context
    private HttpServletRequest request;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        //Allow OPTIONS calls for CORS
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS"))
            return;

        UriInfo uri = requestContext.getUriInfo();

        String org = requestContext.getHeaderString("x-sc-org");
        if(isSet(org))
            LOG.trace("Found incoming sc-org header: " + org);
        if(isEmpty(org)) {
            String host = requestContext.getHeaderString("referer");
            host = isSet(host) ? URI.create(host).getHost() : requestContext.getHeaderString("x-forwarded-host");
            host = isSet(host) ? host : uri.getRequestUri().getHost();
            org = host.split("\\.")[0];
        }

        String uriPath = uri.getPath();
        if(isSet(org) && !uriPath.equals("ping"))
            LOG.trace("enabling org: " + org);

        OrganizationContext.enableOrganization(org);
        ThreadContext.put("request.org", org);

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

        String token = parseOptionalAuthHeader(requestContext);


        RequestType request = new RequestType(requestContext.getMethod().toUpperCase(), uriPath.toLowerCase());
        if(!uriPath.equals("ping"))
            LOG.trace("Authorizing request: " + request);

        // No token required for login and password resets...
        // TODO: Is there a better way to do this with routing?
        if (OPEN_PATHS.contains(request))
            return;

        DecodedJWT jwt = parseAuthToken(token);

        if (jwt == null) {
            if (OPTIONAL_TOKEN_PATHS.contains(request))
                return;
            else
                throw new NotAuthorizedException("Not Authorized");
        }

        String user = getUserName(jwt);
        ThreadContext.put("user", user);

        if(OrganizationContext.isMultiTenant()) {
            Organization activeOrg = OrganizationContext.getOrganization();
            if (activeOrg == null || !activeOrg.getName().equals(getOrg(jwt)))
                throw new NotAuthorizedException("Not Authorized");
        }

        SESSION_VERIFIER.verifySession(callingIp, token, user);

        enableRole(jwt);
        SecurityContext context = createContext(requestContext.getUriInfo(), jwt);
        requestContext.setSecurityContext(context);
    }


    // ----- Private -----
    private String parseOptionalAuthHeader(ContainerRequestContext requestContext) {
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

    private DecodedJWT parseAuthToken(String token) {
        if(isEmpty(token))
            return null;

        try {
            return VERIFIER.verify(token);
        } catch (JWTVerificationException e) {
            LOG.warn("Invalid jwt token presented.", e);
            return null;
        }
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

    private int getUserId(DecodedJWT jwt) {
        Claim claim = jwt.getClaim("userId");
        if(claim == null)
            throw new NotAuthorizedException("Token does not have a userId");

        return claim.asInt();
    }

    private String getUserName(DecodedJWT jwt) {
        return jwt.getSubject();
    }

    private String getOrg(DecodedJWT jwt) {
        Claim claim = jwt.getClaim("org");
        if(claim == null)
            throw new NotAuthorizedException("Token does not have an organization");

        return claim.asString();
    }

    private static class RequestType {
        private final String method;
        private final String path;
        private final boolean partial;

        public RequestType(String method, String path, boolean partial) {
            this.method = method;
            this.path = path;
            this.partial = partial;
        }

        public RequestType(String method, String path) {
            this(method, path, false);
        }

        public RequestType(String path) {
            this("POST", path, false);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof RequestType))
                return false;
            RequestType other = (RequestType)obj;
            return (this.method.equals(ANY) || other.method.equals(ANY) || this.method.equals(other.method)) &&
                        ((this.partial && other.path.startsWith(this.path)) ||
                         (other.partial && this.path.startsWith(other.path)) ||
                         this.path.equals(other.path));
        }

        @Override
        public String toString() {
            return String.format("Request {%s %s}", method, path);
        }
    }
}