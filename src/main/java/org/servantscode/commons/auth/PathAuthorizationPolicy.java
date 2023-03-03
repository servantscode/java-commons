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

public class PathAuthorizationPolicy implements AuthorizationPolicy {
    private static final Logger LOG = LogManager.getLogger(PathAuthorizationPolicy.class);

    //Make sure these lists are lower cased for case-insensitive comparisons
    protected static final List<RequestType> OPTIONAL_TOKEN_PATHS = new LinkedList<>();

    protected static final List<RequestType> OPEN_PATHS = new LinkedList<>();

    public void applyPolicy(ContainerRequestContext requestContext, DecodedJWT jwt) {
        UriInfo uri = requestContext.getUriInfo();
        String uriPath = uri.getPath();

        RequestType request = new RequestType(requestContext.getMethod().toUpperCase(), uriPath.toLowerCase());
        if (jwt == null && !OPEN_PATHS.contains(request) && !OPTIONAL_TOKEN_PATHS.contains(request))
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
