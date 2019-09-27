package org.servantscode.commons.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.servantscode.commons.ObjectMapperFactory;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperContextResolver implements ContextResolver<ObjectMapper> {
    private final ObjectMapper MAPPER = ObjectMapperFactory.getMapper();

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return MAPPER;
    }
}
