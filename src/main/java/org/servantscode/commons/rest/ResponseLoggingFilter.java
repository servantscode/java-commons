package org.servantscode.commons.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(5000)
public class ResponseLoggingFilter implements ContainerResponseFilter {
    private static final Logger LOG = LogManager.getLogger(ResponseLoggingFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {

        String startStr = ThreadContext.get("request.received");
        long runtimeMillis = 0;
        if(startStr == null) {
            runtimeMillis = System.currentTimeMillis() - Long.parseLong(startStr);
            ThreadContext.put("request.runtime", Long.toString(runtimeMillis));
        }

        if(!requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            String msg = String.format("Service %s %s completed.",
                    ThreadContext.get("request.method"),
                    ThreadContext.get("request.path"));
            if(runtimeMillis > 0)
                msg += " Completed in: " + runtimeMillis + " msec. ";
            msg += " Response: " + responseContext.getStatus();

            LOG.info(msg);
        }
    }
}