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
import org.glassfish.jersey.inject.hk2.RequestContext;
import org.servantscode.commons.EnvProperty;
import org.servantscode.commons.Organization;
import org.servantscode.commons.Session;
import org.servantscode.commons.db.SessionDB;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.commons.security.PermissionManager;
import org.servantscode.commons.security.SCSecurityContext;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;
import static org.servantscode.commons.security.SCSecurityContext.SYSTEM;

@Provider
@Priority(2000)
public class AuthFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(AuthFilter.class);

    private static final String SIGNING_KEY = EnvProperty.get("JWT_KEY");

    //Make sure these lists are lower cased for case-insensitive comparisons
    private static final List<RequestType> OPTIONAL_TOKEN_PATHS =
            asList(new RequestType("password"),
                    new RequestType("GET", "photo/public/", true));

    private static final List<RequestType> OPEN_PATHS =
            asList(new RequestType("login"),
                    new RequestType("password/reset"),
                    new RequestType("POST","registration"),
                    new RequestType("GET", "person/maritalstatuses"),
                    new RequestType("GET", "person/ethnicities"),
                    new RequestType("GET", "person/languages"),
                    new RequestType("GET", "person/religions"),
                    new RequestType("GET", "person/specialneeds"),
                    new RequestType("GET", "person/phonenumbertypes"),
                    new RequestType("GET", "organization/active"));

    private static final Algorithm algorithm = Algorithm.HMAC256(SIGNING_KEY);
    private static final JWTVerifier VERIFIER = JWT.require(algorithm)
            .acceptLeeway(1)   //1 sec leeway for date checks to account for clock slop
            .withIssuer("Servant's Code")
            .build();

    @Context
    private HttpServletRequest request;

    private SessionDB db;

    public AuthFilter() {
        this.db = new SessionDB();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        //Allow OPTIONS calls for CORS
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS"))
            return;

        UriInfo uri = requestContext.getUriInfo();

        String org = requestContext.getHeaderString("x-sc-org");
        if(isEmpty(org)) {
            String host = requestContext.getHeaderString("referer");
            host = isSet(host) ? URI.create(host).getHost() : requestContext.getHeaderString("x-forwarded-host");
            host = isSet(host) ? host : uri.getRequestUri().getHost();
            org = host.split("\\.")[0];
        }

        OrganizationContext.enableOrganization(org);
        ThreadContext.put("request.org", org);

        String uriPath = uri.getPath();
        ThreadContext.put("request.received", Long.toString(System.currentTimeMillis()));
        ThreadContext.put("request.method", requestContext.getMethod());
        ThreadContext.put("request.path", uriPath);

        String callingIp = request.getRemoteAddr();
        ThreadContext.put("request.origin", callingIp);

        ThreadContext.put("transaction.id", requestContext.getHeaderString("x-sc-transaction-id"));
        if (isEmpty(ThreadContext.get("transaction.id")))
            ThreadContext.put("transaction.id", UUID.randomUUID().toString());

        String token = parseOptionalAuthHeader(requestContext);


        RequestType request = new RequestType(requestContext.getMethod().toUpperCase(), uriPath.toLowerCase());
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

        Organization activeOrg = OrganizationContext.getOrganization();
        if(activeOrg == null || !activeOrg.getName().equals(getOrg(jwt)))
            throw new NotAuthorizedException("Not Authorized");

        if(!user.equals(SYSTEM)) {
            Session activeSession = db.getSessionByToken(token);
            if (activeSession == null || activeSession.getExpiration().isBefore(ZonedDateTime.now())) {
                throw new NotAuthorizedException("Not Authorized");
            } else if (!activeSession.getIp().equals(callingIp)) {
                LOG.info("Encountered change in calling ip. %s => %s", activeSession.getIp(), callingIp);
                activeSession.setIp(callingIp);
                db.updateCallingIp(activeSession);
            }
        }

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
            return this.method.equals(other.method) &&
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