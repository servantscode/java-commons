package org.servantscode.commons.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.servantscode.commons.StringUtils;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;

@Provider
@Priority(5000)
public class RequestLoggingFilter implements ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(RequestLoggingFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {

        ThreadContext.put("request.received", Long.toString(System.currentTimeMillis()));
        if(!requestContext.getMethod().equalsIgnoreCase("OPTIONS"))
            LOG.info(String.format("Service %s %s initiated.",
                    ThreadContext.get("request.method"),
                    ThreadContext.get("request.path")));
    }
}
