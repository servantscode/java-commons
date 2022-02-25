package org.servantscode.commons.rest;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JSONParseExceptionMapper implements ExceptionMapper<MismatchedInputException> {
    private static final Logger LOG = LogManager.getLogger(JSONParseExceptionMapper.class);

    public Response toResponse(MismatchedInputException exception) {
        LOG.error("Failed to parse incoming JSON payload.", exception);

        return Response.status(Response.Status.BAD_REQUEST).
                entity(exception.getMessage()).
                build();
    }
}
