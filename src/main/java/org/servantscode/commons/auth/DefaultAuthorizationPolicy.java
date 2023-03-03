package org.servantscode.commons.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;

public class DefaultAuthorizationPolicy implements AuthorizationPolicy {
    private static final Logger LOG = LogManager.getLogger(DefaultAuthorizationPolicy.class);

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

    public void applyPolicy(ContainerRequestContext requestContext, DecodedJWT jwt) {
        UriInfo uri = requestContext.getUriInfo();
        String uriPath = uri.getPath();

        RequestType request = new RequestType(requestContext.getMethod().toUpperCase(), uriPath.toLowerCase());
        if (jwt == null && (!OPEN_PATHS.contains(request) && !OPTIONAL_TOKEN_PATHS.contains(request)))
            throw new NotAuthorizedException("Not Authorized");
    }

    public void registerOptionalTokenApi(String method, String path, boolean includeSubPaths) {
        OPTIONAL_TOKEN_PATHS.add(new RequestType(method.toUpperCase(), path.toLowerCase(), includeSubPaths));
        LOG.debug("Added optional token path: " + path);
    }

    public void registerPublicApi(String method, String path, boolean includeSubPaths) {
        OPEN_PATHS.add(new RequestType(method.toUpperCase(), path.toLowerCase(), includeSubPaths));
        LOG.debug("Added open path: " + path);
    }

    public void registerPublicService(String path) {
        registerPublicApi("*", path, true);
    }
}
