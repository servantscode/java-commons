package org.servantscode.commons.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.glassfish.jersey.message.internal.ReaderWriter;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Provider
@Priority(5000)
public class LoggingFilter implements ContainerResponseFilter, ContainerRequestFilter {
    private static final Logger LOG = LogManager.getLogger(LoggingFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        StringBuilder sb = new StringBuilder("HTTP RESPONSE : ");
        sb.append("Header: ").append(responseContext.getHeaders());
        sb.append(" - Entity: ");
        if(responseContext.getEntity() != null )
            sb.append(responseContext.getEntity().toString());
        LOG.info(sb);

        String path = ThreadContext.get("request.path");
        if(requestContext.getMethod().equalsIgnoreCase("OPTIONS") ||
           requestContext.getUriInfo().getPath().equals("ping")) {
            return;
        }

        String startStr = ThreadContext.get("request.received");
        long runtimeMillis = 0;
        if(startStr != null) {
            runtimeMillis = System.currentTimeMillis() - Long.parseLong(startStr);
            ThreadContext.put("request.runtime", Long.toString(runtimeMillis));
        }

        String msg = String.format("%s %s. ",
                ThreadContext.get("request.method"),
                ThreadContext.get("request.path"));
        if(runtimeMillis > 0)
            msg += " Completed in: " + runtimeMillis + " msec.";
        msg += " Response: " + responseContext.getStatus();

        LOG.info(msg);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {

        StringBuilder sb = new StringBuilder("HTTP REQUEST : ");
        sb.append("User: ").append(requestContext.getSecurityContext().getUserPrincipal() == null ? "unknown"
                : requestContext.getSecurityContext().getUserPrincipal());
        sb.append(" - Path: ").append(requestContext.getUriInfo().getPath());

        // Remove Auth headers from logging.
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        headers.remove("Authorization");
        sb.append(" - Header: ").append(headers);

        if (requestContext != null || requestContext.hasEntity()) {
            sb.append("Request payload: ").append(getEntityBody(requestContext));
        }
        LOG.info(sb);
    }

    private String getEntityBody(ContainerRequestContext requestContext)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = requestContext.getEntityStream();

        final StringBuilder b = new StringBuilder();
        try
        {
            ReaderWriter.writeTo(in, out);

            byte[] requestEntity = out.toByteArray();
            if (requestEntity.length == 0)
            {
                b.append(System.lineSeparator());
            }
            else
            {
                b.append(new String(requestEntity)).append(System.lineSeparator());
            }
            // Set the entity stream back as it is read once and is needed in the api.
            requestContext.setEntityStream( new ByteArrayInputStream(requestEntity) );

        } catch (IOException ex) {
            //Handle logging error
        }
        return b.toString();
    }

}