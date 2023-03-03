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

public class DefaultAuthorizationPolicy extends PathAuthorizationPolicy {
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
}
